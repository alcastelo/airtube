# AirTube

AirTube is an easy to use networking library for Android and Java creating a distributed peer-to-peer overlay mesh network with service discovery and asynchronous message-oriented end-to-end connectivity. By combining these three main features into a small and dedicated library, AirTube frees application developers from worrying about networking details like topologies, IP addresses and port numbers, and simply lets them discover "services" and receive data from it, be it local, remote or anywhere in the global AirTube overlay mesh "cloud".

Allthough AirTube is a generic networking library for Java, it is optimized for Android mobile devices and the transmission of real-time multimedia data like streaming audio and video.

AirTube is an Open Souce project and licensed under the LGPL.


## Key Features

 * Distributed Service discovery by "name" and JSON description
 * Distributed Peer-to-peer overlay mesh network independent of IP addresses
 * Non-blocking asynchronous message-oriented networking optimized for multimedia data
 * Integration with Computer Vision libraries and algorithms
 * Efficient and small library (~250KB) for Android and any Java platform

[More Info in the Wiki](https://github.com/thinktube-kobe/airtube/wiki/What-is-AirTube%3F)


## Developments status

AirTube was developed internally over a period of about two years and is now being presented to the public as Open Source. The main features "service discovery", "mesh protocol" and "asynchronous networking" are functional, and some effort has been made to provide a stable API which works well as an Android service and in "pure" Java. We still have ambitious plans to move forward by implementing the following features:

 * End-to-end encryption
 * More advanced service matching and lookup
 * Additional service types, like "Request-Response"
 * Additional transmission types like "delayed delivery" (store & forward)
 * ... 

We welcome contributions and are maintaining the project as open source.


## Requirements

 * Android: Version 4.0.4 or higher
 * Java: Version 1.6 or higher
 * No dependency on other libraries


## Services integrated with AirTube

AirTube is a general purpose networking library but we have developed it with a focus on multimedia data and computer vision. As such we provide the following "services" as part of AirTube:

 * Real-time video stream sending (encoding) from Android with H264 codec
 * Video receiving (decoding) on Android and PC
 * Real-time audio stream using the Opus codec (optional Speex, G711, AAC) including a Jitter Buffer implementation and echo cancellation
 * OpenCV processing on the sender and/or receiver side
 * Face detection
 * ...

More documentation in the [Wiki](https://github.com/thinktube-kobe/airtube/wiki)

