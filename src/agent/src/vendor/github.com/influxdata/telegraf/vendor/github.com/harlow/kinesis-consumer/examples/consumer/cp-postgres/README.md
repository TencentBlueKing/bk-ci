# Consumer with postgres checkpoint

Read records from the Kinesis stream using postgres as checkpoint

## Environment Variables

Export the required environment vars for connecting to the Kinesis stream:

```shell
export AWS_ACCESS_KEY=
export AWS_REGION=
export AWS_SECRET_KEY=
```

## Run the consumer

     go run main.go --app appName --stream streamName --table tableName --connection connectionString
