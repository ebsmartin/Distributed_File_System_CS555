# Chord Peer-to-Peer Network Implementation

## Overview
This project involves implementing a simple peer-to-peer (P2P) network based on the Chord protocol, as part of the CS555 Distributed Systems course at Colorado State University. The Chord protocol provides a way to efficiently locate nodes in a distributed system, where each node is assigned a 32-bit integer identifier.

## Objective
The primary objective of this project is to build the logical overlay for the Chord network and enable efficient traversal, storage, and retrieval of content across the network. This project covers several key aspects of distributed systems including node identification, controller node implementation, finger table management, and handling of node joins and departures.

## Features
- **Node Identification**: Each node, identified by a unique `peerID` generated using the `hashCode()` method on the node's IP and port combination, interacts within the network.
- **Controller Node**: Acts as an initial contact point for nodes joining the system, maintaining a registry of active nodes.
- **Finger Table**: Implements a routing mechanism over a 32-bit identifier space, helping in efficient query resolution.
- **Data Storage and Retrieval**: Utilizes the network's structure to store and retrieve files, with the key based on the hash of the file name.

## Commands
- `peer-nodes`: Lists all peer nodes.
- `neighbors`: Displays information about neighboring nodes.
- `files`: Lists files stored at a node.
- `finger-table`: Shows the finger table entries of a node.
- `upload <file-path>`: Uploads a file to the appropriate node in the Chord system.
- `download <file-name>`: Downloads a file from the Chord system.

## Architecture
The implementation follows a structured overlay network where each node maintains information about a few other nodes in the system (primarily through its finger table), significantly reducing the overhead and improving scalability.

## Usage
To run the project:
1. Start the controller node: `java csx55.chord.ControllerNode <portnum>`
2. Start peer nodes: `java csx55.chord.PeerNode <controller-ip> <controller-port>`

## Contributing
This project is an academic exercise and individual effort was required. Collaboration was limited to architectural discussions.

## License
This project is licensed under the terms of the Colorado State University academic license.

## Acknowledgements
This project is guided by Professor Shrideep Pallickara, based on the principles of the Chord protocol outlined in the original paper by Ion Stoica et al.