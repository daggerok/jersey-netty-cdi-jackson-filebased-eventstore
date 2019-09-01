Use V1 variant if you going to provide only Collection responses

_GET http://127.0.0.1:8080/events/00000000-0000-0000-0000-000000000000/collection_

```json
  {
    "aggregateId": "00000000-0000-0000-0000-000000000000",
    "at": "2019-08-31T22:20:39.653+0000",
    "by": "anonymous",
    "reason": "because!",
    "type": "CounterSuspended"
  }
```

otherwise, you wont see type of event:

_GET http://127.0.0.1:8080/events/00000000-0000-0000-0000-000000000000_

```json
  {
    "aggregateId": "00000000-0000-0000-0000-000000000000",
    "at": "2019-08-31T22:20:39.653+0000",
    "by": "anonymous",
    "reason": "because!"
  }
```
