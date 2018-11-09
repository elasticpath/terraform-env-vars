# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [master](https://github.com/elasticpath/terraform-vars/compare/v1.0.1...HEAD) (unreleased)

## [v1.0.1](https://github.com/elasticpath/terraform-vars/compare/v1.0.0...v1.0.1) (2018-09-17)

### Changed

- Compile with `:advanced` instead of `:simple`

## [v1.0.0](https://github.com/elasticpath/terraform-vars/compare/9e28f16...v1.0.0) (2018-09-12)

### Added

- Accept `--json` option for using a json file as environment map.
- Accept `--edn` file containing a Clojure map from Terraform keyword keys to environment variables string names.
- Introduce `terraform-env-vars` - given `terraform output --no-color --json` of a deployment print out a list of environment variables.

