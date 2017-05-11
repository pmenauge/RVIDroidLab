# RVIDroidLab

Environment to interact with an RVI infrastructure from an Android device.

RVI is a connected car <-> cloud infrastructure typically used with linux-based IVI operating systems like Genivi (genivi.org).
Android is not a target OS for RVI production launch (even though it is technically possible).
However, Android may be practical to emulate a connected car with an Android smartphone as:
* it provides an easy access to several sensors/peripherals common with a car (location, telephony, mobile data, audio output, compass, etc.)
* it provides some caracteristics common with a connected car (mobility, personal device, user interaction, etc.). 

The main intent of this project is to emulate a connected car, in order to test some infrastructure aspects (core functionality, security, service level, device management, etc.).

Current implementation is only partial.

The project is made of two pieces:
* rvidroidlib: communication layer implementing RVI protocols over Json RPC library (jsonrpc4j + )
* rvidroidcar: Android apk providing basic UI for configuring & triggering some services. It uses rvidroidlib to interact with RVI infra.

For runtime, it depends on other RVI open source projects:
* [RVI Core](https://github.com/GENIVI/rvi_core/): RVI core infrastucture, running on both vehicle and backend server. It also provides several documents explaining build, configuration, protocols, security, etc. In addition, some python scripts can be used to send test requests on the infra.
* [RVI Backend](https://github.com/PDXostc/rvi_backend): An example of web frontend to manage an RVI backend infra (provision keys, vehicles, etc.), and to interact with some services (SOTA, location reporting, etc.)

Limitation: The typical deployment is to have rvidroidlib interacting with Service Edge component of the RVI Core instance running on the device as a service. However, I could not finalize this deployment yet as an Erlang interpreter must be first installed on the Android device. I've been facing issues when compiling and installing Erlang+Openssl on my Android 4.4.2 device, I'm currently blocked on a crash when accessing openssl.
For the moment, only the two following deployments are supported:
* DEP#1: Android virtual device (from SDK) interacting with local RVI Core instance running on same PC (and connected to remote RVI backend): supports both accessing remote services and hosting local services.
* DEP#2: Physical Android 4.4.2 device but only interacting with Service Edge of remote RVI backend infra (instead of local RVI core): supports accessing remote services but does not support hosting local services as service name will be prepended with backend prefix and infra will not be able to route requests to the device. Typically, device will be able to report its position to the cloud, however cloud cannot on/off remotelly the service.

## Getting Started

### Prerequisites
For build time:
* Android Studio, Android SDK and Java JDK should be installed on the host PC (vehicle node), typically Ubuntu 16.04 LTS.
* Library dependencies should be added to gradle build system as jar or source files (I've been using source for step by step debugging)

For runtime:
* An rvi_backend and rvi_core project instances should be installed, configured and running on a "backend node" (typically Ubuntu 16.04 LTS). "trackingserver.py" server should be running too to store vehicle reported positions to database.
* For DEP#1 (see above), an instance of rvi_core project should be installed, configured and running on the "vehicle node"
* To change default security related files from rvi_core project, the "scripts/create_new_rvi_security_files.sh" utility can be used to generate new files for backend node and two vehicles nodes.
* Provisionning: connect through the web admin interface on the backend to provision at least one vehicle (security keys and certificate, VIN, description, etc.).
* Edit VehicleConfig.java to change default values to match your environment: VIN, default IP address, etc.

Note that I failed to run two instances of rvi_core on the same node. The launch of the 2nd instance is failing because of duplicate ids. Could not fix issue easily and moved to two separate nodes.


### Installing
APK: Nothing specific, just plug phone before clicking "run" button in Studio if you want to run on physical target. Otherwise I used a virtual device for SDK v17 (Android 4.4.2) as per my physical device.

### Running the tests
* Connection/disconnection with cloud infra
Android: The UI is starting in Disconnected state by default.
Check that IP address is matching the Service Edge backend address and port.
Click "connect" button and check that state is transitionning to CONNECTED. Similar for "disconnect" button.
Click "refresh services list" button to display the list of services available on the RVI infra.
* Vehicle -> Cloud (report vehicle location and speed every X seconds)
Click "start reporting" button to start reporting device position and speed to the cloud. To visualize positions reported on a map, just connect to the Web UI of backend.
* Cloud -> Vehicle
** Remotelly enable/disable the reporting of vehicle positions to the cloud
./rvi_call -n http://192.168.x.x:9001 genivi.org/vin/15232532623621/logging/subscribe channels='["location","speed"]' reporting_interval=5000
./rvi_call -n http://192.168.x.x:9001 genivi.org/vin/15232532623621/logging/unsubscribe channels='["location","speed"]'
** Lock/unlock vehicle doors
./rvi_call -n http://192.168.x.x:9001 genivi.org/vin/15232532623621/control/lock action='"lock"' locks='["doors","trunk"]'
./rvi_call -n http://192.168.x.x:9001 genivi.org/vin/15232532623621/control/lock action='"unlock"' locks='["doors","trunk"]'

### Deployment
See above DEP#1 and DEP#2 possible deployments. Ideal deployment DEP#3 with an RVI Core instance running on the android device is still under debug.

### Built with
*[Jsonrpc4j](https://github.com/briandilley/jsonrpc4j: Json RPC library
*[Jackson Data Binding](https://github.com/FasterXML/jackson-databind: Json library
*[Jackson Core](https://github.com/FasterXML/jackson-core: Json library

### Contributing
Contributions are of course welcome.
Priorities I've in mind right now but open to discussion/change:
* Fix Erlang/Openssl porting issue to enable DEP#3 deployment. Or develop RVI Core in Java...
* Rework UI to split/add multiple decks: core (connect/disconnect/list services), device management (provision keys and certificates), service 1 (reporting), service 2 (lock/unlock), etc.
* Add nicer graphical UI for each service
* Develop scripts for testing
* Develop security specific testing to verify RVI infra robustness

### Versioning

### Authors
Author: Philippe Menauge (menauge@yahoo.com).

### License
GPLv2

### Acknowledgments
Thanks to Mario Giani for his initial support to configure and debug RVI core and backend test infra.








