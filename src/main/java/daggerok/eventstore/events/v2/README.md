Use V2 variant if you will be returning Jersey Response

_GET http://127.0.0.1:8080/events/00000000-0000-0000-0000-000000000000_

```json
  {
    "aggregateId": "00000000-0000-0000-0000-000000000000",
    "at": "2019-08-31T22:20:39.653+0000",
    "by": "anonymous",
    "reason": "because!",
    "type": "CounterSuspended"
  }
```

otherwise, you will ge duplicates, like so:

_GET http://127.0.0.1:8080/events/00000000-0000-0000-0000-000000000000/collection_

```json
  {
    "type": "CounterCreated",
    "aggregateId": "00000000-0000-0000-0000-000000000000",
    "counterName": "hello 1",
    "at": "2019-08-31T22:09:06.541+0000",
    "type": "CounterCreated"
  }
```
