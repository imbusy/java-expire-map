java-expire-map
===============

A map that allows automatic removal of elements after a specified timeout.

Implementation details
----------------------

Inernally uses a ConcurrentHashMap to store the `key, (value, removal timestamp)` values
and a ConcurrentSkipListSet to keep an ordered set of `key, (value, removal timestamp)` ordered
by timestamp to keep track of values to be removed.

An independent thread is created to remove the elements when they reach their removal timestamp.
