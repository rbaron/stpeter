# stpeter

This is part of the toy system built for controlling our office's air conditioner units via Slack. This repo contains the server application, which connects to Slack and also listens for connections from the ESP8266 board.

For more informations on this project, check out [this blog](http://rbaron.net/blog).

## Running

```bash
$ SLACK_API_TOKEN=__MY_SLACK_API_TOKEN__ lein run
```

or use the `lein uberjar` command and run the resulting .jar file.

## License

MIT
