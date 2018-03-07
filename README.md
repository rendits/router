# Rendits Vehicle-to-Anything Router
At [Rendits](http://rendits.com/) we strive to make research in intelligent transportation systems easy. We built this product because we needed a research-friendly platform. We needed something that we could easily customize for whatever project we're currently running. Something built from standardized hardware and without proprietary components. To encourage more open development in the field and as a service to our customers we have chosen to relese this software under the permissive Apache 2.0 license.

Specifically this software is built on top of the [GeoNetworking library](https://github.com/alexvoronov/geonetworking) written by Alex Voronov and it acts as the link between the GeoNetworking stack and the vehicle control system. It was built to make it easy to connect vehicles and other things wirelessly over the [ETSI ITS-G5](http://www.etsi.org/technologies-clusters/technologies/intelligent-transport) standard.

### Link and Hardware Layers
Note that you need special hardware with support of the physical layer. Please see the Github page of the [GeoNetworking](https://github.com/alexvoronov/geonetworking/blob/master/HARDWARE.md) library for an overview on setting up the link and hardware layers.

### Purchasing
The code is open source and you can buy the hardware yourself. If you spend a few weeks reading the specifications, learning how to assemble the software stack and how to recompile the Linux kernel with the proper drivers you could build the product yourself.

We assume you got better things to do. We're all about empowering our users. We want to make you awesome at vehicle-to-vehicle communication without you having to recompile the kernel. Please see [rendits.com](http://rendits.com/) for more information.

### Building, Testing and Running
This project uses [Maven](https://maven.apache.org/) as its build tool. After installing Maven and the [GeoNetworking library](https://github.com/alexvoronov/geonetworking) you can build and run the router via the commands:
```
mvn clean install
java -jar target/rendits-router.jar
```

### License
The Vehicle Adapter is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) license.
