TCPChannel - represents a connection between two nodes
TCPReceiverThread - receiving part of a connection between two nodes
TCPSenderThread - sending part of a connection between two nodes
TCPServerThread - accepts connections
SuccessorReturn - used as a response from the findSuccessor function to either move on or recurse
SuccessorReturnEnum - used as part of SuccessorReturn
DiscoveryRegisterResponse - when a peer node registers with the discovery node, it gets a random node back to help determine the new peer node's position
DiscoveryRegisterResponseOnePeer - when the first peer to join the chord network joins; this special response is used for ease
DownloadForward - forward a download lookup
DownloadRequest - request a download from a node that has data
DownloadResponse - response from a download request
Event - interface for all response types
EventFactory - processes events
Exit - update to some other peers when a peer is exiting the chord ring
ExitRequest - sent to the discovery node
ExitResponse - received as a response to an ExitRequest
FileNotFound - helps display an error code to the requesting node if a file is not found when trying to download a specifically-named file
FileTransferRequest - helps in transferring files when needed
FileTransferResponse - response to a FileTransferRequest
Join - update to some other peers when a peer is joining the chord ring
LookupForward - forwards a lookup when finding finger table entries
PredecessorExitUpdate - helps with notifications when a predecessor is leaving
PredecessorExitUpdateResponse - response to a PredecessorExitUpdate
PredecessorRequest - ask from one peer node to another to receive a predecessor update
PredecessorUpdate - used to update a peer node's predecessor
Protocol - enums for serialization/deserialization
RegisterRequest - used when registering with the Discovery node or another Peer node
SuccessorUpdate - used to update a peer node's successor
UpdateFingerTable - used to update an entry in a peer node's finger table
UploadForward - used to forward a search for placing a file
UploadPlacement - used to place a file
Discovery - node with limited information to help a peer node join the network by giving it 1 random node. Also displays the ip address/port number and ids of peer nodes in the system
Node - interface used by Discovery and Peer nodes
Peer - there are many peer nodes that are in the chord system at specific positions and hold specific data based on their id
