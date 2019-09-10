# Consumer

Read records from the Kinesis stream

### Environment Variables

Export the required environment vars for connecting to the Kinesis stream:

```
export AWS_ACCESS_KEY=
export AWS_REGION=
export AWS_SECRET_KEY=
```

### Run the consumer

    $ go run main.go --app appName --stream streamName --table tableName
