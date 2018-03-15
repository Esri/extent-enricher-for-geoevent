# extent-enricher-for-geoevent

ArcGIS GeoEvent Server sample processor. The Extent Enricher Processor enriches incoming events with the Geometry's extent (MinX, MaxX, MinY, MaxY fields). Optionally, the processor can add the extent's center point as a new Geometry field.

![App](extent-enricher-for-geoevent.png?raw=true)

## Features
* Enriches incoming events with four fields (MinX, MaxX, MinY, MaxY) which represents that event geometry's extent and optionally the extent's center point as a new Geometry field (CenterPoint).

## Instructions

Building the source code:

1. Make sure Maven and ArcGIS GeoEvent Server SDK are installed on your machine.
2. Run 'mvn install -Dcontact.address=[YourContactEmailAddress]'

Installing the built jar files:

1. Copy the *.jar files under the 'target' sub-folder(s) into the [ArcGIS-GeoEvent-Server-Install-Directory]/deploy folder.

## Requirements

* ArcGIS GeoEvent Server for Server (certified with version 10.6.x).
* ArcGIS GeoEvent Server SDK.
* Java JDK 1.7 or greater.
* Maven.

## Resources

* [ArcGIS GeoEvent gallery item](http://www.arcgis.com/home/item.html?id=2bdf684d4b5342e2a8bf6036b6f816b8) on the ArcGIS GeoEvent Gallery
* [ArcGIS GeoEvent Server Resources](http://links.esri.com/geoevent)
* [ArcGIS Blog](http://blogs.esri.com/esri/arcgis/)
* [twitter@esri](http://twitter.com/esri)

## Issues

Find a bug or want to request a new feature?  Please let us know by submitting an issue.

## Contributing

Esri welcomes contributions from anyone and everyone. Please see our [guidelines for contributing](https://github.com/esri/contributing).

## Licensing
Copyright 2015 Esri

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

A copy of the license is available in the repository's [license.txt](license.txt?raw=true) file.
