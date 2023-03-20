# Build main app package
FROM node:18 AS builder

ENV NEXT_TELEMETRY_DISABLED 1

ARG LASIUS_VERSION
LABEL git-commit=$CI_COMMIT_SHORT_SHA

WORKDIR /app

COPY ./ ./

RUN node ./scripts/updatePackageJsonVersion.js $LASIUS_VERSION
RUN yarn workspaces focus
RUN yarn run build
RUN yarn workspaces focus --all --production
RUN rm -rf "$(yarn cache clean)"
RUN rm -rf .git
RUN find . -type f -regextype posix-egrep -regex ".*\.(tsx|ts|tar.gz|md|txt)$" -delete
RUN find . -type f -regextype posix-egrep -regex ".*(LICENSE|license|CHANGELOG)$" -delete

FROM node:18-alpine AS runner
WORKDIR /app

RUN apk add --no-cache tzdata

COPY --from=builder /app ./

CMD yarn run start

# NextJS default port
EXPOSE 3000
