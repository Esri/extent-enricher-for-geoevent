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

import com.esri.ges.core.property.PropertyDefinition;
import com.esri.ges.core.property.PropertyType;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.processor.GeoEventProcessorDefinitionBase;

public class GeometryExtentEnricherDefinition extends GeoEventProcessorDefinitionBase
{
	private static final BundleLogger	LOGGER	= BundleLoggerFactory.getLogger(GeometryExtentEnricherDefinition.class);

	public GeometryExtentEnricherDefinition()
	{
		try
		{
			propertyDefinitions.put("geoEventDefinitionName", new PropertyDefinition("geoEventDefinitionName", PropertyType.String, null, "${com.esri.geoevent.processor.extent-enricher-processor.PROCESSOR_GED_NAME_LBL}", "${com.esri.geoevent.processor.extent-enricher-processor.PROCESSOR_GED_NAME_DESC}", true, false));
			propertyDefinitions.put("addCenterPoint", new PropertyDefinition("addCenterPoint", PropertyType.Boolean, false, "${com.esri.geoevent.processor.extent-enricher-processor.PROCESSOR_ADD_CENTER_PT_LBL}", "${com.esri.geoevent.processor.extent-enricher-processor.PROCESSOR_ADD_CENTER_PT_DESC}", false, false));
		}
		catch (Exception error)
		{
			LOGGER.error("INIT_ERROR", error.getMessage());
			LOGGER.info(error.getMessage(), error);
		}
	}

	@Override
	public String getName()
	{
		return "GeometryExtentEnricher";
	}

	@Override
	public String getDomain()
	{
		return "com.esri.geoevent.processor";
	}

	@Override
	public String getVersion()
	{
		return "10.3.1";
	}

	@Override
	public String getLabel()
	{
		return "${com.esri.geoevent.processor.extent-enricher-processor.PROCESSOR_LABEL}";
	}

	@Override
	public String getDescription()
	{
		return "${com.esri.geoevent.processor.extent-enricher-processor.PROCESSOR_DESC}";
	}
}
