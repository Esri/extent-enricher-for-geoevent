/*
  Copyright 1995-2014 Esri

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
import com.esri.ges.processor.GeoEventProcessorBase;
import com.esri.ges.processor.GeoEventProcessorDefinition;
import com.esri.ges.util.Validator;

public class GeometryExtentEnricher extends GeoEventProcessorBase implements ServiceTrackerCustomizer
{
	private static final BundleLogger	LOGGER		= BundleLoggerFactory.getLogger(GeometryExtentEnricher.class);

	private GeoEventCreator						geoEventCreator;
	private GeoEventDefinitionManager	geoEventDefinitionManager;
	private ServiceTracker						geoEventDefinitionManagerTracker;
	private String										geoEventDefinitionName;

	private Map<String, String>				edMapper	= new ConcurrentHashMap<String, String>();

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
	}

	@Override
	public synchronized void validate() throws ValidationException
	{
		super.validate();
		if (geoEventDefinitionName.isEmpty())
			throw new ValidationException(LOGGER.translate("GED_EMPTY_ERROR"));
	}

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

	private synchronized GeoEventDefinition lookup(GeoEventDefinition edIn) throws Exception
	{
		GeoEventDefinition edOut = edMapper.containsKey(edIn.getGuid()) ? geoEventDefinitionManager.getGeoEventDefinition(edMapper.get(edIn.getGuid())) : null;
		if (edOut == null)
		{
			List<FieldDefinition> newFields = new ArrayList<>();
			newFields.add(new DefaultFieldDefinition("MINX", FieldType.Double));
			newFields.add(new DefaultFieldDefinition("MINY", FieldType.Double));
			newFields.add(new DefaultFieldDefinition("MAXX", FieldType.Double));
			newFields.add(new DefaultFieldDefinition("MAXY", FieldType.Double));

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

	private GeoEvent populateGeoEvent(GeoEvent geoEvent, GeoEventDefinition edOut) throws MessagingException
	{
		GeoEvent outGeoEvent = geoEventCreator.create(edOut.getGuid(), new Object[] { geoEvent.getAllFields(), new Object[4] });
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
				outGeoEvent.setField("MINX", boundingBox.xmin);
				outGeoEvent.setField("MINY", boundingBox.ymin);
				outGeoEvent.setField("MAXX", boundingBox.xmax);
				outGeoEvent.setField("MAXY", boundingBox.ymax);
			}
			catch( FieldException error )
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
