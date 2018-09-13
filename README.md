<img src="https://www.elasticpath.com/sites/all/themes/bootstrap/images/elastlic-path-logo-RGB.svg" alt="elasticpath logo" title="elasticpath" align="right" width="150"/>

### Terraform-env-vars

The `terraform-env-vars` is a small script that receives on `stdin` the
Terraform output in JSON format and from the command line the mapping from
Terraform variable names to environment variable names.

Given the above inputs, it does two things:

 * it validates the Terraform output against the mappings

   **it displays errors and exits with 1 in case of failures**

 * it prints the list of environment variables on `stdout`

   **the variables are separated by `=`, following the [dotenv](https://github.com/motdotla/dotenv) format**

An example of EDN environment map:

```clojure
{:terraform_db_user "PGUSER"
 :terraform_db_passaword "PGPASSWORD"}
```

You can also pass a JSON file by using the `--json` options:

```json
{"terraform_db_user": "PGUSER",
 "terraform_db_passaword": "PGPASSWORD"}
```

Run `terraform-env-vars --help` for more.

#### Usage

```shell
npm install -g @ep-npm/terraform-env-vars -g

cd a-terraform-dir/  # choose your Terraform deployment dir
terraform output -json | terraform-env-vars --edn env-vars.edn
...
PGUSER="..."
PGPASSWORD="..."
...
```

## License

Copyright 2018 Elastic Path

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License.
