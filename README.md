# Lasius open-source time tracking

Lasius is an open source time tracking solution that includes a comprehensive set of features, with a particular focus on team collaboration.

**Public Beta**: We welcome your feedback! Please use the issue tracker of this repository.

Lasius is a modern web application with a backend written in Scala and a NextJS React frontend.

# Features

## Time Tracking

* Start-Stop tracking: Record time spent on a task in real-time
* Labels & Tags: Assign labels to each booking and edit labels on project level
* Favorites: Save your most used bookings as favorites and start booking with one click
* Progressive Web App: Use Lasius on your mobile device as a PWA and add it to your homescreen
* Dark-mode: Switch between light and dark mode
* Statistics & Reports: See your organisation, project or personal statistics for a given time period
* Export: Export organisation, project or personal bookings using various filters as CSV for a given time period
* ACL: Assign roles to users in a project or organisation to allow or restrict access to certain features

## Team Features

* Organisations: Be a member of multiple organisations and invite users with an invitation link, switch between them
  anytime and see only organisation specific data
* Projects: Create projects, assign them to organisations and invite users with an invitation link
* Team View: See what everybody is currently working on and book on the same task with one click

## Integrations

* Issue trackers: Connect your issue tracker to Lasius and use issue numbers as labels. Currently supported:
  * GitLab
  * Jira

## Personal Time Management

* Set your personal hourly target per weekday and organisation
* See your progress in real-time

# Roadmap

We plan to implement the following features in the near future (no specific order, no ETA):

* [ ] Make GitLab and Jira integration configurable in the frontend (currently hardcoded)
* [ ] Add support for GitHub issue tracker
* [ ] Make tags and labels configuration configurable in the frontend (currently hardcoded)
* [ ] Add support for sending E-Mails via SMTP for password reset, invitation links, etc.
* [ ] Quick onboarding for new users with basic usage instructions
* [ ] Special project to book sick days, holidays, etc. per organization

If you plan to use Lasius for your company or organisation, and you depend on one of the above features, we are happy to discuss sponsoring the development.

Watch this repository to get notified about new releases.

# History

The development of Lasius started in 2015. It is the exclusive time tracking tool of Tegonal, an experienced software development team based in Bern (Switzerland). We developed Lasius because there was no tool available in 2015 and, to be honest, because we just wanted to build something new and nice :-)

Our time tracker had to be based on open source components, meet our high privacy standards and be able to be hosted wherever we wanted. The feature set of Lasius has been continuously adapted to our needs in everyday project work and we are happy that we can share it with you.

# Development 
## Requirements

* mongoDB >= 5.0.9

## Environment Variables

This is only necessary if you sping up the containers manually or with your own compose file. For your convenience,
check out the [lasius-docker-compose](https://github.com/tegonal/lasius-docker-compose) companion repo.
Please see the `docker-compose.yml` file for container specific environment variables.

The following variables are suggested to be used in an `.env` file alongside docker-compose and could be used by all
containers, containing secrets that only might be available during CI/CD.

| Variable name              | Description                                                                                                             | Default value            |
|----------------------------|-------------------------------------------------------------------------------------------------------------------------|--------------------------|
| LASIUS_HOSTNAME            | Hostname (i.e. localhost, domain.com, ...)                                                                              | localhost                |
| LASIUS_VERSION             | The current version, corresponds with docker image tags. We suggest using specific versions in production, not `latest` | latest                   |
| MONGODB_URI                | Override connection to mongodb                                                                                          | see `docker-compose.yml` |
| MONGO_INITDB_PASSWORD      | Password of mongoDB user                                                                                                | lasius                   |
| MONGO_INITDB_ROOT_PASSWORD | Password of root user of mongoDB                                                                                        | admin                    |
| MONGO_INITDB_ROOT_USERNAME | Username of root user of mongoDB                                                                                        | admin                    |
| MONGO_INITDB_USERNAME      | Username of mongoDB user                                                                                                | lasius                   |
| NEXT_AUTH_SECRET           | Hash for next-auth session salting, e.g. the output of `openssl rand -base64 32`                                        | random string            |
| TRAEFIK_CERT_EMAIL         | E-mail address to use when fetching a certificate from LE                                                               | ssladmin@lasius.ch       |
| TRAEFIK_CERT_RESOLVER      | LetsEncrypt resolver, use `letsencrpyt` in production, empty value for testing (mind the LE rate limit)                 | letsencrypt              |
| TZ                         | Your desired timezone                                                                                                   | CET                      |

Specific to `backend` container:

| Variable name                    | Description                                                                                                                                                    | Default value            |
|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------|
| LASIUS_CLEAN_DATABASE_ON_STARTUP | If true, drop on startup all data                                                                                                                              | 'false'                  |
| LASIUS_INITIALIZE_DATA           | `'true'` if database should automatically get initialized in case no user accounts are configured                                                              | 'true'                   |
| LASIUS_INITIAL_USER_EMAIL        | Username of initial admin user to login. Only used when `LASIUS_INITIALIZE_DATA` is set to `'true'` and no users where found in the database.                  | admin@lasius.ch          |
| LASIUS_INITIAL_USER_KEY          | Initial internal user key for to the intial user account. Only used when `LASIUS_INITIALIZE_DATA` is set to `'true'` and no users where found in the database. | admin                    |
| LASIUS_INITIAL_USER_PASSWORD     | Password of initial admin user to login. Only used when `LASIUS_INITIALIZE_DATA` is set to `true` and no users where found in the database.                    | admin                    |
| LASIUS_START_PARAMS              | Provide special start arguments to the play server. Might be used to inject a different `application.conf` to the server.                                      | see `docker-compose.yml` |
| LASIUS_SUPPORTS_TRANSACTIONS     | To be able to benefit of transactions in MongoDB you need a replica set first.                                                                                 | 'false'                  |

Specific to `frontend` container:

| Variable name                | Description                                                                                 | Default value |
|------------------------------|---------------------------------------------------------------------------------------------|---------------|
| ENVIRONMENT                  | `production` - any other value runs NextJS in dev mode. Not suggested in deployments.       | production    |
| NEXT_AUTH_SECRET             | Hash for next-auth session salting, e.g. the output of `openssl rand -base64 32`            | random string |
| LASIUS_DEMO_MODE             | Enables or disables demo mode                                                               | `false`       |
| LASIUS_TELEMETRY_MATOMO_HOST | Hostname/FQDN of a matomo instance to collect anonymous usage data, e.g. `stats.domain.com` | undefined     |
| LASIUS_TELEMETRY_MATOMO_ID   | Matomo site ID, e.g. `42`                                                                   | undefined     |

We suggest you use a `.env` file and save it in the same directory as the `docker-compose.yml` for build dependent configuration and edit all other variables in the `docker-compose.yml` file directly if they are not dependent on CI/CD variables.

## Dev Environment

To bring up a local dev Environment please install:

- sbt
- docker
- node

Start the backend with `yarn run backend` and the frontend with `yarn run dev` from the `frontend` directory.

## Test Environment

To simply bring up a test environment, check out
the [lasius-docker-compose](https://github.com/tegonal/lasius-docker-compose) companion repo.

## Production Environment

To bring up a production environment, check out
the [lasius-docker-compose](https://github.com/tegonal/lasius-docker-compose) companion repo.

The docker-compose setup above comes with single mongoDB instance and therefore without support of transactions. To use Lasius in production, you should use transactions and therefore run mongoDB in a replicaset. To benefit from transactions in Mongo DB you need to set `LASIUS_SUPPORTS_TRANSACTIONS=true` and configure an external access to the mongodb replicaset through `MONGODB_URI`.

Lasius' docker-compose.yml supports LetsEncrypt certificates out of the box, thanks to Traefik reverse proxy. If you decide to run Lasius behind another reverse proxy or SSL termination point, you can look at `docker-compose-no-https.yml`. However, we strongly suggest using secure connections.

# License

As we are strongly committed to open source software, we make Lasius available to the community under [AGPLv3](https://www.gnu.org/licenses/agpl-3.0.en.html) license. The code in this repo is provided without warranty.

# Support

If you would like us to set up or run Lasius for you then please contact us here for an offer: <https://tegonal.com>

If you need help, discover a bug or have a feature request, please open an issue in this repo.
