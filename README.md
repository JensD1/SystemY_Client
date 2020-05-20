# SystemY_Client
This is a distributed file node that will exist in a network. A lot of other instances of this project, called nodes, will also be present in this network. Nodes can join and leave the network.

The network will also contain a Naming Server (which can be found at https://github.com/JensD1/SystemY_Server).

The purpose is that the nodes posess files that are replicated and redundantly saved, so we won't lose those files by failure. Furthermore nodes can request files and the namingserver will then determine where the files are saved. 

When files are added or deleted, these will again be replicated to the other nodes or deleted on all nodes that posess a replication of these files.

The user can execute all commands via a Textual User Interface and can see messages via a logger.
