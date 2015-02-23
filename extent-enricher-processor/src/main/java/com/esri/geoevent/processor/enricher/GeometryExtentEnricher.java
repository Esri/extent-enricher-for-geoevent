/*
  Copyright 1995-2015 Esri

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

  For additional information, contact:
  Environmental Systems Research Institute, Inc.
  Attn: Contracts Dept
  380 New York Street
  Redlands, California, USA 92373

  email: contracts@esri.com
 */

package com.esri.geoevent.processor.enricher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.MapGeometry;
import com.esri.core.geometry.Point;
import com.esri.ges.core.component.ComponentException;
import com.esri.ges.core.geoevent.DefaultFieldDefinition;
import com.esri.ges.core.geoevent.FieldDefinition;
import com.esri.ges.core.geoevent.FieldException;
import com.esri.ges.core.geoevent.FieldType;
import com.esri.ges.core.geoevent.GeoEvent;
import com.esri.ges.core.geoevent.GeoEventDefinition;
import com.esri.ges.core.geoevent.GeoEventPropertyName;
import com.esri.ges.core.validation.ValidationException;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.manager.geoeventdefinition.GeoEventDefinitionManager;
import com.esri.ges.manager.geoeventdefinition.GeoEventDefinitionManagerException;
import com.esri.ges.messaging.GeoEventCreator;
import com.esri.ges.messaging.Messaging;
import com.esri.ges.messaging.MessagingException;
import com.esri.ges.processor.GeoEventProcessor;
import com.esri.ges.processor.GeoEventProcessorBase;
import com.esri.ges.processor.GeoEventProcessorDefinition;
import com.esri.ges.util.Converter;
import com.esri.ges.util.Validator;

public class GeometryExtentEnricher extends GeoEventProcessorBase implements ServiceTrackerCustomizer
{
	private static final BundleLogger	LOGGER					= BundleLoggerFactory.getLogger(GeometryExtentEnricher.class);

	private GeoEventCreator						geoEventCreator;
	private GeoEventDefinitionManager	geoEventDefinitionManager;
	private ServiceTracker						geoEventDefinitionManagerTracker;
	private String										geoEventDefinitionName;
	private Boolean										addCenterPoint	= false;

	private Map<String, String>				edMapper				= new ConcurrentHashMap<String, String>();

	protected GeometryExtentEnricher(GeoEventProcessorDefinition definition) throws ComponentException
	{
		super(definition);
		if (geoEventDefinitionManagerTracker == null)
			geoEventDefinitionManagerTracker = new ServiceTracker(definition.getBundleContext(), GeoEventDefinitionManager.class.getName(), this);
		geoEventDefinitionManagerTracker.open();
	}

	public void afterPropertiesSet()
	{
		geoEventDefinitionName = Validator.compactWhiteSpaces(getProperty("geoEventDefinitionName").getValueAsString());
		addCenterPoint = Converter.convertToBoolean(getProperty("addCenterPoint").getValueAsString());
	}

	@Override
	public synchronized void validate() throws ValidationException
	{
		super.validate();
		if (geoEventDefinitionName.isEmpty())
			throw new ValidationException(LOGGER.translate("GED_EMPTY_ERROR"));
	}

	/**
	 * This is the main override method for {@link GeoEventProcessor}. It calls two methods,
	 * {@link #lookup(GeoEventDefinition)} and {@link #populateGeoEvent(GeoEvent, GeoEventDefinition)} which do most of
	 * the work.
	 * 
	 * @param geoevent
	 *          of type {@link GeoEvent}.
	 * @throws Exception
	 */
	@Override
	public GeoEvent process(GeoEvent geoEvent) throws Exception
	{
		GeoEvent augmentedGeoEvent = geoEvent;
		if (geoEvent != null && geoEventDefinitionManager != null)
		{
			GeoEventDefinition edOut = lookup(geoEvent.getGeoEventDefinition());
			augmentedGeoEvent = populateGeoEvent(geoEvent, edOut);
		}
		return augmentedGeoEvent;
	}

	/**
	 * <p>
	 * The lookup method is used to search the {@link GeoEventDefinitionManager} for the configurable
	 * {@link GeoEventDefinition} via the property <code>geoEventDefinitionName</code>. When it is is found, it will
	 * augment the {@link GeoEventDefinition} with the new Extent related fields <code>MinX</code>, <code>MinY</code>,
	 * <code>MaxX</code>, <code>MaxY</code> (all of type {@link FieldType#Double}). The augmented
	 * {@link GeoEventDefinition} will be added as a new "temporary" {@link GeoEventDefinitionManager} to the
	 * GeoEventDefinitionManager via
	 * {@link GeoEventDefinitionManager#addTemporaryGeoEventDefinition(GeoEventDefinition, boolean)}.
	 * </p>
	 * 
	 * <p>
	 * <b>Optionally</b> if the configuration property <code>addCenterPoint</code> was set to <code>true</code>, the field
	 * <code>CenterPoint</code> of type {@link FieldType#Geometry} will be added as well.
	 * </p>
	 * 
	 * @param edIn the GeoEventDefinition to augment.
	 * @return the augmented GeoEventDefinition 
	 * @throws Exception if the incoming GeoEventDefinition cannot be augmented or created.
	 * 
	 * @see GeoEventDefinition
	 * @see GeoEventDefinitionManager
	 */
	private synchronized GeoEventDefinition lookup(GeoEventDefinition edIn) throws Exception
	{
		GeoEventDefinition edOut = edMapper.containsKey(edIn.getGuid()) ? geoEventDefinitionManager.getGeoEventDefinition(edMapper.get(edIn.getGuid())) : null;
		if (edOut == null)
		{
			List<FieldDefinition> newFields = new ArrayList<>();
			newFields.add(new DefaultFieldDefinition("MinX", FieldType.Double));
			newFields.add(new DefaultFieldDefinition("MinY", FieldType.Double));
			newFields.add(new DefaultFieldDefinition("MaxX", FieldType.Double));
			newFields.add(new DefaultFieldDefinition("MaxY", FieldType.Double));
			if (addCenterPoint)
				newFields.add(new DefaultFieldDefinition("CenterPoint", FieldType.Geometry));

			edOut = edIn.augment(newFields);
			edOut.setOwner(getId());
			if (StringUtils.isNotBlank(geoEventDefinitionName))
			{
				edOut.setName(geoEventDefinitionName);
				geoEventDefinitionManager.addTemporaryGeoEventDefinition(edOut, false);
			}
			else
				geoEventDefinitionManager.addTemporaryGeoEventDefinition(edOut, true);
			edMapper.put(edIn.getGuid(), edOut.getGuid());
		}
		return edOut;
	}

	/**
	 * The populateGeoEvent method does the following things:
	 * <ol>
	 * 	<li>Creates a copy of the incoming GeoEvent using the {@link GeoEventCreator}.</li>
	 * 	<li>Check's the GeoEvent's Geometry (it make sure it exists) and creates an extent from it.</li>
	 * 	<li>Adds the fields <code>MinX</code>, <code>MinY</code>, <code>MaxX</code>, <code>MaxY</code></li> to the GeoEvent using the Geometry's extent.
	 * 	<li>Optionally, adds the  Geometry's extent center point as well.</li>
	 * </ol>
	 * 
	 * @param geoEvent The incoming GeoEvent to be augmented
	 * @param edOut the augmented GeoEventDefinition
	 * @return the augmented GeoEvent
	 * 
	 * @throws MessagingException if the incoming GeoEvent cannot be augmented.
	 * 
	 * @see GeoEvent
	 * @see GeoEventDefinition
	 */
	private GeoEvent populateGeoEvent(GeoEvent geoEvent, GeoEventDefinition edOut) throws MessagingException
	{
		GeoEvent outGeoEvent = geoEventCreator.create(edOut.getGuid(), new Object[] { geoEvent.getAllFields(), (addCenterPoint) ? new Object[5] : new Object[4] });
		outGeoEvent.setProperty(GeoEventPropertyName.TYPE, geoEvent.getProperty(GeoEventPropertyName.TYPE));
		outGeoEvent.setProperty(GeoEventPropertyName.OWNER_ID, geoEvent.getProperty(GeoEventPropertyName.OWNER_ID));
		outGeoEvent.setProperty(GeoEventPropertyName.OWNER_URI, geoEvent.getProperty(GeoEventPropertyName.OWNER_URI));

		MapGeometry geometry = geoEvent.getGeometry();
		if (geometry != null)
		{
			Envelope2D boundingBox = new Envelope2D();
			geometry.getGeometry().queryEnvelope2D(boundingBox);

			try
			{
				outGeoEvent.setField("MinX", boundingBox.xmin);
				outGeoEvent.setField("MinY", boundingBox.ymin);
				outGeoEvent.setField("MaxX", boundingBox.xmax);
				outGeoEvent.setField("MaxY", boundingBox.ymax);
				if (addCenterPoint)
				{
					Point centerPt = new Point(boundingBox.getCenter());
					outGeoEvent.setField("CenterPoint", new MapGeometry(centerPt, geometry.getSpatialReference()));
				}
			}
			catch (FieldException error)
			{
				LOGGER.error("ERROR_SETTING_EXTENT_FIELDS", error.getMessage());
				LOGGER.info(error.getMessage(), error);
			}
		}
		else
			LOGGER.debug("GEOMETRY_EMPTY_MSG");
		return outGeoEvent;
	}

	@Override
	public Object addingService(ServiceReference serviceReference)
	{
		Object service = definition.getBundleContext().getService(serviceReference);
		if (service instanceof GeoEventDefinitionManager)
			this.geoEventDefinitionManager = (GeoEventDefinitionManager) service;
		return service;
	}

	@Override
	public void modifiedService(ServiceReference serviceReference, Object service)
	{
		;
	}

	@Override
	public void removedService(ServiceReference serviceReference, Object service)
	{
		if (service instanceof GeoEventDefinitionManager)
		{
			clearGeoEventDefinitionMapper();
			this.geoEventDefinitionManager = null;
		}
	}

	synchronized private void clearGeoEventDefinitionMapper()
	{
		if (!edMapper.isEmpty())
		{
			for (String guid : edMapper.values())
			{
				try
				{
					geoEventDefinitionManager.deleteGeoEventDefinition(guid);
				}
				catch (GeoEventDefinitionManagerException error)
				{
					LOGGER.warn("PROCESSOR_FAILED_TO_DELETE_GED", guid, error.getMessage());
				}
			}
			edMapper.clear();
		}
	}

	public void setMessaging(Messaging messaging)
	{
		geoEventCreator = messaging.createGeoEventCreator();
	}
}
