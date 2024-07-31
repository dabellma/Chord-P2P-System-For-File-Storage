This is an implementation of the Chord peer-to-peer system. In this implementation, each node has a 32-bit unique identifier, which means this system can support up to ~4 billion nodes. As files are put into the system to be saved, they are processed and forwarded to the appropriate node by determining their unique identifier. As nodes join or leave the system, files are moved in accordance with the node identifier that corresponds to the file identifier. This way files are stored efficiently with no nodes containing too many files.
