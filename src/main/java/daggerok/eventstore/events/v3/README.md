Use V3 variant if you will be returning Collection of DomainEvents (Result is similar to V1)

_GET http://127.0.0.1:8080/events/00000000-0000-0000-0000-000000000000/collection_

```json
  {
    "type": "CounterCreated",
    "aggregateId": "00000000-0000-0000-0000-000000000000",
    "counterName": "hello 1",
    "at": "2019-08-31T22:09:06.541+0000"
  }
```

otherwise, you will see in response less info, like so:

_GET http://127.0.0.1:8080/events/00000000-0000-0000-0000-000000000000_

```json
  {
    "aggregateId": "00000000-0000-0000-0000-000000000000",
    "counterName": "hello 1",
    "at": "2019-08-31T22:09:06.541+0000"
  }
```
