# MonkeyCI Pushover Notifier

This is a small Clojure app that listens to [MonkeyCI](https://www.monkeyci.com) events
and sends a notification whenever a build is started or completed.  It posts messages
to [Pushover](https://pushover.net) whenever an event for the configured customer has
been received.

Ideally configuration is dynamic, so clients can "register" as needed, but it will
probably be static initially.  We will use `edn` files to store the config, and we can
switch to a regular database later, or flush the config to a bucket and load it from
there on restart.

# License

Copyright (c) 2024 by ]Monkey Projects BV](https://www.monkey-projects.be)
[MIT license](LICENSE)
