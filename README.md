# SystemY
This is a distributed file server.

##TODO
- HASHING (seperate class) -> Samer

- REST NAMING SERVER (namingserver.java)
- NODE CLIENT (nodeclient.java)

- DISCOVERY (nodeclient.java + namingseerver.java)
=> from new Node send multicast message to all existing nodes + naming server with his name and ip address

- BOOTSTRAP (nodeclient.java)
=> init/update node parameters
=> all nodes calculate their own hash, store local information from other nodes: previousID < currentID < nextID (see Discovery and Bootstrap slide 5)
=> when new node sends broadcast, all nodes check previousID and nextID form hash value
=> New node processes response from naming sever (based on #existing nodes)


- SHUTDOWN (nodeclient.java)
=> send  updated param to neigbours before leaving network

- FAILURE (nodeclient.java + namingserver.java)
=>  namingserver has to handle parameter updates

FIX multicasthandler in nodeclient for endnodes!