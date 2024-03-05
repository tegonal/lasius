# Lasius Frontend

This project should run cross-platform but is currently only being tested and used on Linux and MacOS, including the M1 variety.

## Prerequisites

```
> nvm --version
0.39.7
```
```
> node --version
v20.11.0
```
```
> docker --version
Docker version 20.10.14, build a224086`
```

```
npm i -g yarn
```

Yarn 3.x is installed as a local dependency within this directory (`.yarn`), your global yarn v1 will pick up this version whenever launched in this directory.

All dev dependencies (`prettier`, `eslint`, `concurrently`, ...) are available as local dependencies within this project.
You don't need to (shouldn't) use your global installs of these.


## Install

Browse to this folder and run `yarn`, after you made sure your system has the required software mentioned above.


## Development

`yarn run backend`

Launches current backend from `../backend` (specifically `sbt run`) and two docker containers: Nginx to be used as transparent
proxy and mongoDB. The backend will populate itself with demo data and pregenerate views on the first request.

`yarn run dev`

Run NextJS in development mode with live reload etc.

Hint: Nginx proxies both backend and frontend requests, so make sure to start both scripts in order to have a running dev env.

Once all service are online, browse to

`http://localhost:3000` - NextJS in development mode
`http://localhost:9000/backend/docs/swagger-ui/index.html?url=/backend/assets/swagger.json` - OpenAPI UI
`http://localhost:9000/backend/` - Summary of Play routes

You can use the following demo users to login:

```
demo1@lasius.ch
demo
```
```
demo2@lasius.ch
demo
```


## Build

`yarn run build` builds locally, using the development environment.

See `package.json` for more build options and options to start the server locally.


## Chores

### Updating dependencies

`yarn run up`

Interactive dependency updates.

Please note that the following packages are currently pinned to their major version and should not be updated:

`@mdx-js/react` as peer dependency of themeUI
`@types/node` tied to the system node version, usually the current Node JS LTS release in use

`yarn run openapi`

Re-generates the TypeScript API client. Requires `yarn run backend` to be running in the background.


### Cleaning house

`yarn run cleaner`

Removes all packages, dist files, lock files, package cache et al. Reinstalls dependencies, often resolving to newer peer deps.

`yarn rebuild`

Rebuilds only platform specific binaries found in dependencies.

`yarn run lint`

Lint the source.


## Project structure

`/@types` TS global overrides/interfaces

`/env` dotenv files providing environment variables for multiple environments

`/public` static files that are reachable from the servers html root, like images, fonts, et al

`/scripts` build and helper scripts fir build tasks

`/src` source root, React components

`/src/pages` NextJS routes and API routes

`/src/messages/{country code}.json` UI strings and translations
