# Group-Messenger2
This is an Android Application which allows communication between multiple devices and provides Total and FIFO ordering guarantees. 
A FIFO ordered protocol guarantees that messages by the same sender are delivered in the order that they were sent. That is, if a process multicasts a message m before it multicasts a message m', then no correct process receives m' unless it has previously received m
Total Ordering guarantees that all correct processes receive all messages in the same order. That is, if correct processes p and q both receive messages m and m', then p receives m before m' if and only if q receives m before m'. The multicast is atomic across all members of the group.
