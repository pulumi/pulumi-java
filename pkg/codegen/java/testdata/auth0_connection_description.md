With Auth0, you can define sources of users, otherwise known as connections, which may include identity providers (such as Google or LinkedIn), databases, or passwordless authentication methods. This resource allows you to configure and manage connections to be used with your clients and users.

\u003e The Auth0 dashboard displays only one connection per social provider. Although the Auth0 Management API allows the
creation of multiple connections per strategy, the additional connections may not be visible in the Auth0 dashboard.


## Example Usage

### Auth0 Connection

\u003c!--Start PulumiCodeChooser --\u003e
```typescript
import * as pulumi from "@pulumi/pulumi";
import * as auth0 from "@pulumi/auth0";

// This is an example of an Auth0 connection.
const myConnection = new auth0.Connection("my_connection", {
    name: "Example-Connection",
    isDomainConnection: true,
    strategy: "auth0",
    metadata: {
        key1: "foo",
        key2: "bar",
    },
    options: {
        passwordPolicy: "excellent",
        bruteForceProtection: true,
        enabledDatabaseCustomization: true,
        importMode: false,
        requiresUsername: true,
        disableSignup: false,
        customScripts: {
            get_user: `        function getByEmail(email, callback) {
          return callback(new Error("Whoops!"));
        }
`,
        },
        configuration: {
            foo: "bar",
            bar: "baz",
        },
        upstreamParams: JSON.stringify({
            screen_name: {
                alias: "login_hint",
            },
        }),
        passwordHistories: [{
            enable: true,
            size: 3,
        }],
        passwordNoPersonalInfo: {
            enable: true,
        },
        passwordDictionary: {
            enable: true,
            dictionaries: [
                "password",
                "admin",
                "1234",
            ],
        },
        passwordComplexityOptions: {
            minLength: 12,
        },
        validation: {
            username: {
                min: 10,
                max: 40,
            },
        },
        mfa: {
            active: true,
            returnEnrollSettings: true,
        },
    },
});
```
```python
import pulumi
import json
import pulumi_auth0 as auth0

# This is an example of an Auth0 connection.
my_connection = auth0.Connection("my_connection",
    name="Example-Connection",
    is_domain_connection=True,
    strategy="auth0",
    metadata={
        "key1": "foo",
        "key2": "bar",
    },
    options=auth0.ConnectionOptionsArgs(
        password_policy="excellent",
        brute_force_protection=True,
        enabled_database_customization=True,
        import_mode=False,
        requires_username=True,
        disable_signup=False,
        custom_scripts={
            "get_user": """        function getByEmail(email, callback) {
          return callback(new Error("Whoops!"));
        }
""",
        },
        configuration={
            "foo": "bar",
            "bar": "baz",
        },
        upstream_params=json.dumps({
            "screen_name": {
                "alias": "login_hint",
            },
        }),
        password_histories=[auth0.ConnectionOptionsPasswordHistoryArgs(
            enable=True,
            size=3,
        )],
        password_no_personal_info=auth0.ConnectionOptionsPasswordNoPersonalInfoArgs(
            enable=True,
        ),
        password_dictionary=auth0.ConnectionOptionsPasswordDictionaryArgs(
            enable=True,
            dictionaries=[
                "password",
                "admin",
                "1234",
            ],
        ),
        password_complexity_options=auth0.ConnectionOptionsPasswordComplexityOptionsArgs(
            min_length=12,
        ),
        validation=auth0.ConnectionOptionsValidationArgs(
            username=auth0.ConnectionOptionsValidationUsernameArgs(
                min=10,
                max=40,
            ),
        ),
        mfa=auth0.ConnectionOptionsMfaArgs(
            active=True,
            return_enroll_settings=True,
        ),
    ))
```
```csharp
using System.Collections.Generic;
using System.Linq;
using System.Text.Json;
using Pulumi;
using Auth0 = Pulumi.Auth0;

return await Deployment.RunAsync(() =\u003e 
{
    // This is an example of an Auth0 connection.
    var myConnection = new Auth0.Connection("my_connection", new()
    {
        Name = "Example-Connection",
        IsDomainConnection = true,
        Strategy = "auth0",
        Metadata = 
        {
            { "key1", "foo" },
            { "key2", "bar" },
        },
        Options = new Auth0.Inputs.ConnectionOptionsArgs
        {
            PasswordPolicy = "excellent",
            BruteForceProtection = true,
            EnabledDatabaseCustomization = true,
            ImportMode = false,
            RequiresUsername = true,
            DisableSignup = false,
            CustomScripts = 
            {
                { "get_user", @"        function getByEmail(email, callback) {
          return callback(new Error(""Whoops!""));
        }
" },
            },
            Configuration = 
            {
                { "foo", "bar" },
                { "bar", "baz" },
            },
            UpstreamParams = JsonSerializer.Serialize(new Dictionary\u003cstring, object?\u003e
            {
                ["screen_name"] = new Dictionary\u003cstring, object?\u003e
                {
                    ["alias"] = "login_hint",
                },
            }),
            PasswordHistories = new[]
            {
                new Auth0.Inputs.ConnectionOptionsPasswordHistoryArgs
                {
                    Enable = true,
                    Size = 3,
                },
            },
            PasswordNoPersonalInfo = new Auth0.Inputs.ConnectionOptionsPasswordNoPersonalInfoArgs
            {
                Enable = true,
            },
            PasswordDictionary = new Auth0.Inputs.ConnectionOptionsPasswordDictionaryArgs
            {
                Enable = true,
                Dictionaries = new[]
                {
                    "password",
                    "admin",
                    "1234",
                },
            },
            PasswordComplexityOptions = new Auth0.Inputs.ConnectionOptionsPasswordComplexityOptionsArgs
            {
                MinLength = 12,
            },
            Validation = new Auth0.Inputs.ConnectionOptionsValidationArgs
            {
                Username = new Auth0.Inputs.ConnectionOptionsValidationUsernameArgs
                {
                    Min = 10,
                    Max = 40,
                },
            },
            Mfa = new Auth0.Inputs.ConnectionOptionsMfaArgs
            {
                Active = true,
                ReturnEnrollSettings = true,
            },
        },
    });

});
```
```go
package main

import (
	"encoding/json"

	"github.com/pulumi/pulumi-auth0/sdk/v3/go/auth0"
	"github.com/pulumi/pulumi/sdk/v3/go/pulumi"
)

func main() {
	pulumi.Run(func(ctx *pulumi.Context) error {
		tmpJSON0, err := json.Marshal(map[string]interface{}{
			"screen_name": map[string]interface{}{
				"alias": "login_hint",
			},
		})
		if err != nil {
			return err
		}
		json0 := string(tmpJSON0)
		// This is an example of an Auth0 connection.
		_, err = auth0.NewConnection(ctx, "my_connection", \u0026auth0.ConnectionArgs{
			Name:               pulumi.String("Example-Connection"),
			IsDomainConnection: pulumi.Bool(true),
			Strategy:           pulumi.String("auth0"),
			Metadata: pulumi.StringMap{
				"key1": pulumi.String("foo"),
				"key2": pulumi.String("bar"),
			},
			Options: \u0026auth0.ConnectionOptionsArgs{
				PasswordPolicy:               pulumi.String("excellent"),
				BruteForceProtection:         pulumi.Bool(true),
				EnabledDatabaseCustomization: pulumi.Bool(true),
				ImportMode:                   pulumi.Bool(false),
				RequiresUsername:             pulumi.Bool(true),
				DisableSignup:                pulumi.Bool(false),
				CustomScripts: pulumi.StringMap{
					"get_user": pulumi.String("        function getByEmail(email, callback) {\
          return callback(new Error(\\"Whoops!\\"));\
        }\
"),
				},
				Configuration: pulumi.Map{
					"foo": pulumi.Any("bar"),
					"bar": pulumi.Any("baz"),
				},
				UpstreamParams: pulumi.String(json0),
				PasswordHistories: auth0.ConnectionOptionsPasswordHistoryArray{
					\u0026auth0.ConnectionOptionsPasswordHistoryArgs{
						Enable: pulumi.Bool(true),
						Size:   pulumi.Int(3),
					},
				},
				PasswordNoPersonalInfo: \u0026auth0.ConnectionOptionsPasswordNoPersonalInfoArgs{
					Enable: pulumi.Bool(true),
				},
				PasswordDictionary: \u0026auth0.ConnectionOptionsPasswordDictionaryArgs{
					Enable: pulumi.Bool(true),
					Dictionaries: pulumi.StringArray{
						pulumi.String("password"),
						pulumi.String("admin"),
						pulumi.String("1234"),
					},
				},
				PasswordComplexityOptions: \u0026auth0.ConnectionOptionsPasswordComplexityOptionsArgs{
					MinLength: pulumi.Int(12),
				},
				Validation: \u0026auth0.ConnectionOptionsValidationArgs{
					Username: \u0026auth0.ConnectionOptionsValidationUsernameArgs{
						Min: pulumi.Int(10),
						Max: pulumi.Int(40),
					},
				},
				Mfa: \u0026auth0.ConnectionOptionsMfaArgs{
					Active:               pulumi.Bool(true),
					ReturnEnrollSettings: pulumi.Bool(true),
				},
			},
		})
		if err != nil {
			return err
		}
		return nil
	})
}
```
```java
package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.auth0.Connection;
import com.pulumi.auth0.ConnectionArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsPasswordNoPersonalInfoArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsPasswordDictionaryArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsPasswordComplexityOptionsArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsValidationArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsValidationUsernameArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsMfaArgs;
import static com.pulumi.codegen.internal.Serialization.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        // This is an example of an Auth0 connection.
        var myConnection = new Connection("myConnection", ConnectionArgs.builder()        
            .name("Example-Connection")
            .isDomainConnection(true)
            .strategy("auth0")
            .metadata(Map.ofEntries(
                Map.entry("key1", "foo"),
                Map.entry("key2", "bar")
            ))
            .options(ConnectionOptionsArgs.builder()
                .passwordPolicy("excellent")
                .bruteForceProtection(true)
                .enabledDatabaseCustomization(true)
                .importMode(false)
                .requiresUsername(true)
                .disableSignup(false)
                .customScripts(Map.of("get_user", """
        function getByEmail(email, callback) {
          return callback(new Error("Whoops!"));
        }
                """))
                .configuration(Map.ofEntries(
                    Map.entry("foo", "bar"),
                    Map.entry("bar", "baz")
                ))
                .upstreamParams(serializeJson(
                    jsonObject(
                        jsonProperty("screen_name", jsonObject(
                            jsonProperty("alias", "login_hint")
                        ))
                    )))
                .passwordHistories(ConnectionOptionsPasswordHistoryArgs.builder()
                    .enable(true)
                    .size(3)
                    .build())
                .passwordNoPersonalInfo(ConnectionOptionsPasswordNoPersonalInfoArgs.builder()
                    .enable(true)
                    .build())
                .passwordDictionary(ConnectionOptionsPasswordDictionaryArgs.builder()
                    .enable(true)
                    .dictionaries(                    
                        "password",
                        "admin",
                        "1234")
                    .build())
                .passwordComplexityOptions(ConnectionOptionsPasswordComplexityOptionsArgs.builder()
                    .minLength(12)
                    .build())
                .validation(ConnectionOptionsValidationArgs.builder()
                    .username(ConnectionOptionsValidationUsernameArgs.builder()
                        .min(10)
                        .max(40)
                        .build())
                    .build())
                .mfa(ConnectionOptionsMfaArgs.builder()
                    .active(true)
                    .returnEnrollSettings(true)
                    .build())
                .build())
            .build());

    }
}
```
```yaml
resources:
  # This is an example of an Auth0 connection.
  myConnection:
    type: auth0:Connection
    name: my_connection
    properties:
      name: Example-Connection
      isDomainConnection: true
      strategy: auth0
      metadata:
        key1: foo
        key2: bar
      options:
        passwordPolicy: excellent
        bruteForceProtection: true
        enabledDatabaseCustomization: true
        importMode: false
        requiresUsername: true
        disableSignup: false
        customScripts:
          get_user: |2
                    function getByEmail(email, callback) {
                      return callback(new Error("Whoops!"));
                    }
        configuration:
          foo: bar
          bar: baz
        upstreamParams:
          fn::toJSON:
            screen_name:
              alias: login_hint
        passwordHistories:
          - enable: true
            size: 3
        passwordNoPersonalInfo:
          enable: true
        passwordDictionary:
          enable: true
          dictionaries:
            - password
            - admin
            - '1234'
        passwordComplexityOptions:
          minLength: 12
        validation:
          username:
            min: 10
            max: 40
        mfa:
          active: true
          returnEnrollSettings: true
```
\u003c!--End PulumiCodeChooser --\u003e

### Google OAuth2 Connection

\u003e Your Auth0 account may be pre-configured with a `google-oauth2` connection.

\u003c!--Start PulumiCodeChooser --\u003e
```typescript
import * as pulumi from "@pulumi/pulumi";
import * as auth0 from "@pulumi/auth0";

// This is an example of a Google OAuth2 connection.
const googleOauth2 = new auth0.Connection("google_oauth2", {
    name: "Google-OAuth2-Connection",
    strategy: "google-oauth2",
    options: {
        clientId: "\u003cclient-id\u003e",
        clientSecret: "\u003cclient-secret\u003e",
        allowedAudiences: [
            "example.com",
            "api.example.com",
        ],
        scopes: [
            "email",
            "profile",
            "gmail",
            "youtube",
        ],
        setUserRootAttributes: "on_each_login",
        nonPersistentAttrs: [
            "ethnicity",
            "gender",
        ],
    },
});
```
```python
import pulumi
import pulumi_auth0 as auth0

# This is an example of a Google OAuth2 connection.
google_oauth2 = auth0.Connection("google_oauth2",
    name="Google-OAuth2-Connection",
    strategy="google-oauth2",
    options=auth0.ConnectionOptionsArgs(
        client_id="\u003cclient-id\u003e",
        client_secret="\u003cclient-secret\u003e",
        allowed_audiences=[
            "example.com",
            "api.example.com",
        ],
        scopes=[
            "email",
            "profile",
            "gmail",
            "youtube",
        ],
        set_user_root_attributes="on_each_login",
        non_persistent_attrs=[
            "ethnicity",
            "gender",
        ],
    ))
```
```csharp
using System.Collections.Generic;
using System.Linq;
using Pulumi;
using Auth0 = Pulumi.Auth0;

return await Deployment.RunAsync(() =\u003e 
{
    // This is an example of a Google OAuth2 connection.
    var googleOauth2 = new Auth0.Connection("google_oauth2", new()
    {
        Name = "Google-OAuth2-Connection",
        Strategy = "google-oauth2",
        Options = new Auth0.Inputs.ConnectionOptionsArgs
        {
            ClientId = "\u003cclient-id\u003e",
            ClientSecret = "\u003cclient-secret\u003e",
            AllowedAudiences = new[]
            {
                "example.com",
                "api.example.com",
            },
            Scopes = new[]
            {
                "email",
                "profile",
                "gmail",
                "youtube",
            },
            SetUserRootAttributes = "on_each_login",
            NonPersistentAttrs = new[]
            {
                "ethnicity",
                "gender",
            },
        },
    });

});
```
```go
package main

import (
	"github.com/pulumi/pulumi-auth0/sdk/v3/go/auth0"
	"github.com/pulumi/pulumi/sdk/v3/go/pulumi"
)

func main() {
	pulumi.Run(func(ctx *pulumi.Context) error {
		// This is an example of a Google OAuth2 connection.
		_, err := auth0.NewConnection(ctx, "google_oauth2", \u0026auth0.ConnectionArgs{
			Name:     pulumi.String("Google-OAuth2-Connection"),
			Strategy: pulumi.String("google-oauth2"),
			Options: \u0026auth0.ConnectionOptionsArgs{
				ClientId:     pulumi.String("\u003cclient-id\u003e"),
				ClientSecret: pulumi.String("\u003cclient-secret\u003e"),
				AllowedAudiences: pulumi.StringArray{
					pulumi.String("example.com"),
					pulumi.String("api.example.com"),
				},
				Scopes: pulumi.StringArray{
					pulumi.String("email"),
					pulumi.String("profile"),
					pulumi.String("gmail"),
					pulumi.String("youtube"),
				},
				SetUserRootAttributes: pulumi.String("on_each_login"),
				NonPersistentAttrs: pulumi.StringArray{
					pulumi.String("ethnicity"),
					pulumi.String("gender"),
				},
			},
		})
		if err != nil {
			return err
		}
		return nil
	})
}
```
```java
package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.auth0.Connection;
import com.pulumi.auth0.ConnectionArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        // This is an example of a Google OAuth2 connection.
        var googleOauth2 = new Connection("googleOauth2", ConnectionArgs.builder()        
            .name("Google-OAuth2-Connection")
            .strategy("google-oauth2")
            .options(ConnectionOptionsArgs.builder()
                .clientId("\u003cclient-id\u003e")
                .clientSecret("\u003cclient-secret\u003e")
                .allowedAudiences(                
                    "example.com",
                    "api.example.com")
                .scopes(                
                    "email",
                    "profile",
                    "gmail",
                    "youtube")
                .setUserRootAttributes("on_each_login")
                .nonPersistentAttrs(                
                    "ethnicity",
                    "gender")
                .build())
            .build());

    }
}
```
```yaml
resources:
  # This is an example of a Google OAuth2 connection.
  googleOauth2:
    type: auth0:Connection
    name: google_oauth2
    properties:
      name: Google-OAuth2-Connection
      strategy: google-oauth2
      options:
        clientId: \u003cclient-id\u003e
        clientSecret: \u003cclient-secret\u003e
        allowedAudiences:
          - example.com
          - api.example.com
        scopes:
          - email
          - profile
          - gmail
          - youtube
        setUserRootAttributes: on_each_login
        nonPersistentAttrs:
          - ethnicity
          - gender
```
\u003c!--End PulumiCodeChooser --\u003e

### Google Apps

\u003c!--Start PulumiCodeChooser --\u003e
```typescript
import * as pulumi from "@pulumi/pulumi";
import * as auth0 from "@pulumi/auth0";

const googleApps = new auth0.Connection("google_apps", {
    name: "connection-google-apps",
    isDomainConnection: false,
    strategy: "google-apps",
    showAsButton: false,
    options: {
        clientId: "",
        clientSecret: "",
        domain: "example.com",
        tenantDomain: "example.com",
        domainAliases: [
            "example.com",
            "api.example.com",
        ],
        apiEnableUsers: true,
        scopes: [
            "ext_profile",
            "ext_groups",
        ],
        iconUrl: "https://example.com/assets/logo.png",
        upstreamParams: JSON.stringify({
            screen_name: {
                alias: "login_hint",
            },
        }),
        setUserRootAttributes: "on_each_login",
        nonPersistentAttrs: [
            "ethnicity",
            "gender",
        ],
    },
});
```
```python
import pulumi
import json
import pulumi_auth0 as auth0

google_apps = auth0.Connection("google_apps",
    name="connection-google-apps",
    is_domain_connection=False,
    strategy="google-apps",
    show_as_button=False,
    options=auth0.ConnectionOptionsArgs(
        client_id="",
        client_secret="",
        domain="example.com",
        tenant_domain="example.com",
        domain_aliases=[
            "example.com",
            "api.example.com",
        ],
        api_enable_users=True,
        scopes=[
            "ext_profile",
            "ext_groups",
        ],
        icon_url="https://example.com/assets/logo.png",
        upstream_params=json.dumps({
            "screen_name": {
                "alias": "login_hint",
            },
        }),
        set_user_root_attributes="on_each_login",
        non_persistent_attrs=[
            "ethnicity",
            "gender",
        ],
    ))
```
```csharp
using System.Collections.Generic;
using System.Linq;
using System.Text.Json;
using Pulumi;
using Auth0 = Pulumi.Auth0;

return await Deployment.RunAsync(() =\u003e 
{
    var googleApps = new Auth0.Connection("google_apps", new()
    {
        Name = "connection-google-apps",
        IsDomainConnection = false,
        Strategy = "google-apps",
        ShowAsButton = false,
        Options = new Auth0.Inputs.ConnectionOptionsArgs
        {
            ClientId = "",
            ClientSecret = "",
            Domain = "example.com",
            TenantDomain = "example.com",
            DomainAliases = new[]
            {
                "example.com",
                "api.example.com",
            },
            ApiEnableUsers = true,
            Scopes = new[]
            {
                "ext_profile",
                "ext_groups",
            },
            IconUrl = "https://example.com/assets/logo.png",
            UpstreamParams = JsonSerializer.Serialize(new Dictionary\u003cstring, object?\u003e
            {
                ["screen_name"] = new Dictionary\u003cstring, object?\u003e
                {
                    ["alias"] = "login_hint",
                },
            }),
            SetUserRootAttributes = "on_each_login",
            NonPersistentAttrs = new[]
            {
                "ethnicity",
                "gender",
            },
        },
    });

});
```
```go
package main

import (
	"encoding/json"

	"github.com/pulumi/pulumi-auth0/sdk/v3/go/auth0"
	"github.com/pulumi/pulumi/sdk/v3/go/pulumi"
)

func main() {
	pulumi.Run(func(ctx *pulumi.Context) error {
		tmpJSON0, err := json.Marshal(map[string]interface{}{
			"screen_name": map[string]interface{}{
				"alias": "login_hint",
			},
		})
		if err != nil {
			return err
		}
		json0 := string(tmpJSON0)
		_, err = auth0.NewConnection(ctx, "google_apps", \u0026auth0.ConnectionArgs{
			Name:               pulumi.String("connection-google-apps"),
			IsDomainConnection: pulumi.Bool(false),
			Strategy:           pulumi.String("google-apps"),
			ShowAsButton:       pulumi.Bool(false),
			Options: \u0026auth0.ConnectionOptionsArgs{
				ClientId:     pulumi.String(""),
				ClientSecret: pulumi.String(""),
				Domain:       pulumi.String("example.com"),
				TenantDomain: pulumi.String("example.com"),
				DomainAliases: pulumi.StringArray{
					pulumi.String("example.com"),
					pulumi.String("api.example.com"),
				},
				ApiEnableUsers: pulumi.Bool(true),
				Scopes: pulumi.StringArray{
					pulumi.String("ext_profile"),
					pulumi.String("ext_groups"),
				},
				IconUrl:               pulumi.String("https://example.com/assets/logo.png"),
				UpstreamParams:        pulumi.String(json0),
				SetUserRootAttributes: pulumi.String("on_each_login"),
				NonPersistentAttrs: pulumi.StringArray{
					pulumi.String("ethnicity"),
					pulumi.String("gender"),
				},
			},
		})
		if err != nil {
			return err
		}
		return nil
	})
}
```
```java
package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.auth0.Connection;
import com.pulumi.auth0.ConnectionArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
import static com.pulumi.codegen.internal.Serialization.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        var googleApps = new Connection("googleApps", ConnectionArgs.builder()        
            .name("connection-google-apps")
            .isDomainConnection(false)
            .strategy("google-apps")
            .showAsButton(false)
            .options(ConnectionOptionsArgs.builder()
                .clientId("")
                .clientSecret("")
                .domain("example.com")
                .tenantDomain("example.com")
                .domainAliases(                
                    "example.com",
                    "api.example.com")
                .apiEnableUsers(true)
                .scopes(                
                    "ext_profile",
                    "ext_groups")
                .iconUrl("https://example.com/assets/logo.png")
                .upstreamParams(serializeJson(
                    jsonObject(
                        jsonProperty("screen_name", jsonObject(
                            jsonProperty("alias", "login_hint")
                        ))
                    )))
                .setUserRootAttributes("on_each_login")
                .nonPersistentAttrs(                
                    "ethnicity",
                    "gender")
                .build())
            .build());

    }
}
```
```yaml
resources:
  googleApps:
    type: auth0:Connection
    name: google_apps
    properties:
      name: connection-google-apps
      isDomainConnection: false
      strategy: google-apps
      showAsButton: false
      options:
        clientId:
        clientSecret:
        domain: example.com
        tenantDomain: example.com
        domainAliases:
          - example.com
          - api.example.com
        apiEnableUsers: true
        scopes:
          - ext_profile
          - ext_groups
        iconUrl: https://example.com/assets/logo.png
        upstreamParams:
          fn::toJSON:
            screen_name:
              alias: login_hint
        setUserRootAttributes: on_each_login
        nonPersistentAttrs:
          - ethnicity
          - gender
```
\u003c!--End PulumiCodeChooser --\u003e

### Facebook Connection

\u003c!--Start PulumiCodeChooser --\u003e
```typescript
import * as pulumi from "@pulumi/pulumi";
import * as auth0 from "@pulumi/auth0";

// This is an example of a Facebook connection.
const facebook = new auth0.Connection("facebook", {
    name: "Facebook-Connection",
    strategy: "facebook",
    options: {
        clientId: "\u003cclient-id\u003e",
        clientSecret: "\u003cclient-secret\u003e",
        scopes: [
            "public_profile",
            "email",
            "groups_access_member_info",
            "user_birthday",
        ],
        setUserRootAttributes: "on_each_login",
        nonPersistentAttrs: [
            "ethnicity",
            "gender",
        ],
    },
});
```
```python
import pulumi
import pulumi_auth0 as auth0

# This is an example of a Facebook connection.
facebook = auth0.Connection("facebook",
    name="Facebook-Connection",
    strategy="facebook",
    options=auth0.ConnectionOptionsArgs(
        client_id="\u003cclient-id\u003e",
        client_secret="\u003cclient-secret\u003e",
        scopes=[
            "public_profile",
            "email",
            "groups_access_member_info",
            "user_birthday",
        ],
        set_user_root_attributes="on_each_login",
        non_persistent_attrs=[
            "ethnicity",
            "gender",
        ],
    ))
```
```csharp
using System.Collections.Generic;
using System.Linq;
using Pulumi;
using Auth0 = Pulumi.Auth0;

return await Deployment.RunAsync(() =\u003e 
{
    // This is an example of a Facebook connection.
    var facebook = new Auth0.Connection("facebook", new()
    {
        Name = "Facebook-Connection",
        Strategy = "facebook",
        Options = new Auth0.Inputs.ConnectionOptionsArgs
        {
            ClientId = "\u003cclient-id\u003e",
            ClientSecret = "\u003cclient-secret\u003e",
            Scopes = new[]
            {
                "public_profile",
                "email",
                "groups_access_member_info",
                "user_birthday",
            },
            SetUserRootAttributes = "on_each_login",
            NonPersistentAttrs = new[]
            {
                "ethnicity",
                "gender",
            },
        },
    });

});
```
```go
package main

import (
	"github.com/pulumi/pulumi-auth0/sdk/v3/go/auth0"
	"github.com/pulumi/pulumi/sdk/v3/go/pulumi"
)

func main() {
	pulumi.Run(func(ctx *pulumi.Context) error {
		// This is an example of a Facebook connection.
		_, err := auth0.NewConnection(ctx, "facebook", \u0026auth0.ConnectionArgs{
			Name:     pulumi.String("Facebook-Connection"),
			Strategy: pulumi.String("facebook"),
			Options: \u0026auth0.ConnectionOptionsArgs{
				ClientId:     pulumi.String("\u003cclient-id\u003e"),
				ClientSecret: pulumi.String("\u003cclient-secret\u003e"),
				Scopes: pulumi.StringArray{
					pulumi.String("public_profile"),
					pulumi.String("email"),
					pulumi.String("groups_access_member_info"),
					pulumi.String("user_birthday"),
				},
				SetUserRootAttributes: pulumi.String("on_each_login"),
				NonPersistentAttrs: pulumi.StringArray{
					pulumi.String("ethnicity"),
					pulumi.String("gender"),
				},
			},
		})
		if err != nil {
			return err
		}
		return nil
	})
}
```
```java
package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.auth0.Connection;
import com.pulumi.auth0.ConnectionArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        // This is an example of a Facebook connection.
        var facebook = new Connection("facebook", ConnectionArgs.builder()        
            .name("Facebook-Connection")
            .strategy("facebook")
            .options(ConnectionOptionsArgs.builder()
                .clientId("\u003cclient-id\u003e")
                .clientSecret("\u003cclient-secret\u003e")
                .scopes(                
                    "public_profile",
                    "email",
                    "groups_access_member_info",
                    "user_birthday")
                .setUserRootAttributes("on_each_login")
                .nonPersistentAttrs(                
                    "ethnicity",
                    "gender")
                .build())
            .build());

    }
}
```
```yaml
resources:
  # This is an example of a Facebook connection.
  facebook:
    type: auth0:Connection
    properties:
      name: Facebook-Connection
      strategy: facebook
      options:
        clientId: \u003cclient-id\u003e
        clientSecret: \u003cclient-secret\u003e
        scopes:
          - public_profile
          - email
          - groups_access_member_info
          - user_birthday
        setUserRootAttributes: on_each_login
        nonPersistentAttrs:
          - ethnicity
          - gender
```
\u003c!--End PulumiCodeChooser --\u003e

### Apple Connection

\u003c!--Start PulumiCodeChooser --\u003e
```typescript
import * as pulumi from "@pulumi/pulumi";
import * as auth0 from "@pulumi/auth0";

// This is an example of an Apple connection.
const apple = new auth0.Connection("apple", {
    name: "Apple-Connection",
    strategy: "apple",
    options: {
        clientId: "\u003cclient-id\u003e",
        clientSecret: `-----BEGIN PRIVATE KEY-----
MIHBAgEAMA0GCSqGSIb3DQEBAQUABIGsMIGpAgEAA
-----END PRIVATE KEY-----`,
        teamId: "\u003cteam-id\u003e",
        keyId: "\u003ckey-id\u003e",
        scopes: [
            "email",
            "name",
        ],
        setUserRootAttributes: "on_first_login",
        nonPersistentAttrs: [
            "ethnicity",
            "gender",
        ],
    },
});
```
```python
import pulumi
import pulumi_auth0 as auth0

# This is an example of an Apple connection.
apple = auth0.Connection("apple",
    name="Apple-Connection",
    strategy="apple",
    options=auth0.ConnectionOptionsArgs(
        client_id="\u003cclient-id\u003e",
        client_secret="""-----BEGIN PRIVATE KEY-----
MIHBAgEAMA0GCSqGSIb3DQEBAQUABIGsMIGpAgEAA
-----END PRIVATE KEY-----""",
        team_id="\u003cteam-id\u003e",
        key_id="\u003ckey-id\u003e",
        scopes=[
            "email",
            "name",
        ],
        set_user_root_attributes="on_first_login",
        non_persistent_attrs=[
            "ethnicity",
            "gender",
        ],
    ))
```
```csharp
using System.Collections.Generic;
using System.Linq;
using Pulumi;
using Auth0 = Pulumi.Auth0;

return await Deployment.RunAsync(() =\u003e 
{
    // This is an example of an Apple connection.
    var apple = new Auth0.Connection("apple", new()
    {
        Name = "Apple-Connection",
        Strategy = "apple",
        Options = new Auth0.Inputs.ConnectionOptionsArgs
        {
            ClientId = "\u003cclient-id\u003e",
            ClientSecret = @"-----BEGIN PRIVATE KEY-----
MIHBAgEAMA0GCSqGSIb3DQEBAQUABIGsMIGpAgEAA
-----END PRIVATE KEY-----",
            TeamId = "\u003cteam-id\u003e",
            KeyId = "\u003ckey-id\u003e",
            Scopes = new[]
            {
                "email",
                "name",
            },
            SetUserRootAttributes = "on_first_login",
            NonPersistentAttrs = new[]
            {
                "ethnicity",
                "gender",
            },
        },
    });

});
```
```go
package main

import (
	"github.com/pulumi/pulumi-auth0/sdk/v3/go/auth0"
	"github.com/pulumi/pulumi/sdk/v3/go/pulumi"
)

func main() {
	pulumi.Run(func(ctx *pulumi.Context) error {
		// This is an example of an Apple connection.
		_, err := auth0.NewConnection(ctx, "apple", \u0026auth0.ConnectionArgs{
			Name:     pulumi.String("Apple-Connection"),
			Strategy: pulumi.String("apple"),
			Options: \u0026auth0.ConnectionOptionsArgs{
				ClientId:     pulumi.String("\u003cclient-id\u003e"),
				ClientSecret: pulumi.String("-----BEGIN PRIVATE KEY-----\
MIHBAgEAMA0GCSqGSIb3DQEBAQUABIGsMIGpAgEAA\
-----END PRIVATE KEY-----"),
				TeamId:       pulumi.String("\u003cteam-id\u003e"),
				KeyId:        pulumi.String("\u003ckey-id\u003e"),
				Scopes: pulumi.StringArray{
					pulumi.String("email"),
					pulumi.String("name"),
				},
				SetUserRootAttributes: pulumi.String("on_first_login"),
				NonPersistentAttrs: pulumi.StringArray{
					pulumi.String("ethnicity"),
					pulumi.String("gender"),
				},
			},
		})
		if err != nil {
			return err
		}
		return nil
	})
}
```
```java
package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.auth0.Connection;
import com.pulumi.auth0.ConnectionArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        // This is an example of an Apple connection.
        var apple = new Connection("apple", ConnectionArgs.builder()        
            .name("Apple-Connection")
            .strategy("apple")
            .options(ConnectionOptionsArgs.builder()
                .clientId("\u003cclient-id\u003e")
                .clientSecret("""
-----BEGIN PRIVATE KEY-----
MIHBAgEAMA0GCSqGSIb3DQEBAQUABIGsMIGpAgEAA
-----END PRIVATE KEY-----                """)
                .teamId("\u003cteam-id\u003e")
                .keyId("\u003ckey-id\u003e")
                .scopes(                
                    "email",
                    "name")
                .setUserRootAttributes("on_first_login")
                .nonPersistentAttrs(                
                    "ethnicity",
                    "gender")
                .build())
            .build());

    }
}
```
```yaml
resources:
  # This is an example of an Apple connection.
  apple:
    type: auth0:Connection
    properties:
      name: Apple-Connection
      strategy: apple
      options:
        clientId: \u003cclient-id\u003e
        clientSecret: |-
          -----BEGIN PRIVATE KEY-----
          MIHBAgEAMA0GCSqGSIb3DQEBAQUABIGsMIGpAgEAA
          -----END PRIVATE KEY-----
        teamId: \u003cteam-id\u003e
        keyId: \u003ckey-id\u003e
        scopes:
          - email
          - name
        setUserRootAttributes: on_first_login
        nonPersistentAttrs:
          - ethnicity
          - gender
```
\u003c!--End PulumiCodeChooser --\u003e

### LinkedIn Connection

\u003c!--Start PulumiCodeChooser --\u003e
```typescript
import * as pulumi from "@pulumi/pulumi";
import * as auth0 from "@pulumi/auth0";

// This is an example of an LinkedIn connection.
const linkedin = new auth0.Connection("linkedin", {
    name: "Linkedin-Connection",
    strategy: "linkedin",
    options: {
        clientId: "\u003cclient-id\u003e",
        clientSecret: "\u003cclient-secret\u003e",
        strategyVersion: 2,
        scopes: [
            "basic_profile",
            "profile",
            "email",
        ],
        setUserRootAttributes: "on_each_login",
        nonPersistentAttrs: [
            "ethnicity",
            "gender",
        ],
    },
});
```
```python
import pulumi
import pulumi_auth0 as auth0

# This is an example of an LinkedIn connection.
linkedin = auth0.Connection("linkedin",
    name="Linkedin-Connection",
    strategy="linkedin",
    options=auth0.ConnectionOptionsArgs(
        client_id="\u003cclient-id\u003e",
        client_secret="\u003cclient-secret\u003e",
        strategy_version=2,
        scopes=[
            "basic_profile",
            "profile",
            "email",
        ],
        set_user_root_attributes="on_each_login",
        non_persistent_attrs=[
            "ethnicity",
            "gender",
        ],
    ))
```
```csharp
using System.Collections.Generic;
using System.Linq;
using Pulumi;
using Auth0 = Pulumi.Auth0;

return await Deployment.RunAsync(() =\u003e 
{
    // This is an example of an LinkedIn connection.
    var linkedin = new Auth0.Connection("linkedin", new()
    {
        Name = "Linkedin-Connection",
        Strategy = "linkedin",
        Options = new Auth0.Inputs.ConnectionOptionsArgs
        {
            ClientId = "\u003cclient-id\u003e",
            ClientSecret = "\u003cclient-secret\u003e",
            StrategyVersion = 2,
            Scopes = new[]
            {
                "basic_profile",
                "profile",
                "email",
            },
            SetUserRootAttributes = "on_each_login",
            NonPersistentAttrs = new[]
            {
                "ethnicity",
                "gender",
            },
        },
    });

});
```
```go
package main

import (
	"github.com/pulumi/pulumi-auth0/sdk/v3/go/auth0"
	"github.com/pulumi/pulumi/sdk/v3/go/pulumi"
)

func main() {
	pulumi.Run(func(ctx *pulumi.Context) error {
		// This is an example of an LinkedIn connection.
		_, err := auth0.NewConnection(ctx, "linkedin", \u0026auth0.ConnectionArgs{
			Name:     pulumi.String("Linkedin-Connection"),
			Strategy: pulumi.String("linkedin"),
			Options: \u0026auth0.ConnectionOptionsArgs{
				ClientId:        pulumi.String("\u003cclient-id\u003e"),
				ClientSecret:    pulumi.String("\u003cclient-secret\u003e"),
				StrategyVersion: pulumi.Int(2),
				Scopes: pulumi.StringArray{
					pulumi.String("basic_profile"),
					pulumi.String("profile"),
					pulumi.String("email"),
				},
				SetUserRootAttributes: pulumi.String("on_each_login"),
				NonPersistentAttrs: pulumi.StringArray{
					pulumi.String("ethnicity"),
					pulumi.String("gender"),
				},
			},
		})
		if err != nil {
			return err
		}
		return nil
	})
}
```
```java
package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.auth0.Connection;
import com.pulumi.auth0.ConnectionArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        // This is an example of an LinkedIn connection.
        var linkedin = new Connection("linkedin", ConnectionArgs.builder()        
            .name("Linkedin-Connection")
            .strategy("linkedin")
            .options(ConnectionOptionsArgs.builder()
                .clientId("\u003cclient-id\u003e")
                .clientSecret("\u003cclient-secret\u003e")
                .strategyVersion(2)
                .scopes(                
                    "basic_profile",
                    "profile",
                    "email")
                .setUserRootAttributes("on_each_login")
                .nonPersistentAttrs(                
                    "ethnicity",
                    "gender")
                .build())
            .build());

    }
}
```
```yaml
resources:
  # This is an example of an LinkedIn connection.
  linkedin:
    type: auth0:Connection
    properties:
      name: Linkedin-Connection
      strategy: linkedin
      options:
        clientId: \u003cclient-id\u003e
        clientSecret: \u003cclient-secret\u003e
        strategyVersion: 2
        scopes:
          - basic_profile
          - profile
          - email
        setUserRootAttributes: on_each_login
        nonPersistentAttrs:
          - ethnicity
          - gender
```
\u003c!--End PulumiCodeChooser --\u003e

### GitHub Connection

\u003c!--Start PulumiCodeChooser --\u003e
```typescript
import * as pulumi from "@pulumi/pulumi";
import * as auth0 from "@pulumi/auth0";

// This is an example of an GitHub connection.
const github = new auth0.Connection("github", {
    name: "GitHub-Connection",
    strategy: "github",
    options: {
        clientId: "\u003cclient-id\u003e",
        clientSecret: "\u003cclient-secret\u003e",
        scopes: [
            "email",
            "profile",
            "public_repo",
            "repo",
        ],
        setUserRootAttributes: "on_each_login",
        nonPersistentAttrs: [
            "ethnicity",
            "gender",
        ],
    },
});
```
```python
import pulumi
import pulumi_auth0 as auth0

# This is an example of an GitHub connection.
github = auth0.Connection("github",
    name="GitHub-Connection",
    strategy="github",
    options=auth0.ConnectionOptionsArgs(
        client_id="\u003cclient-id\u003e",
        client_secret="\u003cclient-secret\u003e",
        scopes=[
            "email",
            "profile",
            "public_repo",
            "repo",
        ],
        set_user_root_attributes="on_each_login",
        non_persistent_attrs=[
            "ethnicity",
            "gender",
        ],
    ))
```
```csharp
using System.Collections.Generic;
using System.Linq;
using Pulumi;
using Auth0 = Pulumi.Auth0;

return await Deployment.RunAsync(() =\u003e 
{
    // This is an example of an GitHub connection.
    var github = new Auth0.Connection("github", new()
    {
        Name = "GitHub-Connection",
        Strategy = "github",
        Options = new Auth0.Inputs.ConnectionOptionsArgs
        {
            ClientId = "\u003cclient-id\u003e",
            ClientSecret = "\u003cclient-secret\u003e",
            Scopes = new[]
            {
                "email",
                "profile",
                "public_repo",
                "repo",
            },
            SetUserRootAttributes = "on_each_login",
            NonPersistentAttrs = new[]
            {
                "ethnicity",
                "gender",
            },
        },
    });

});
```
```go
package main

import (
	"github.com/pulumi/pulumi-auth0/sdk/v3/go/auth0"
	"github.com/pulumi/pulumi/sdk/v3/go/pulumi"
)

func main() {
	pulumi.Run(func(ctx *pulumi.Context) error {
		// This is an example of an GitHub connection.
		_, err := auth0.NewConnection(ctx, "github", \u0026auth0.ConnectionArgs{
			Name:     pulumi.String("GitHub-Connection"),
			Strategy: pulumi.String("github"),
			Options: \u0026auth0.ConnectionOptionsArgs{
				ClientId:     pulumi.String("\u003cclient-id\u003e"),
				ClientSecret: pulumi.String("\u003cclient-secret\u003e"),
				Scopes: pulumi.StringArray{
					pulumi.String("email"),
					pulumi.String("profile"),
					pulumi.String("public_repo"),
					pulumi.String("repo"),
				},
				SetUserRootAttributes: pulumi.String("on_each_login"),
				NonPersistentAttrs: pulumi.StringArray{
					pulumi.String("ethnicity"),
					pulumi.String("gender"),
				},
			},
		})
		if err != nil {
			return err
		}
		return nil
	})
}
```
```java
package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.auth0.Connection;
import com.pulumi.auth0.ConnectionArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        // This is an example of an GitHub connection.
        var github = new Connection("github", ConnectionArgs.builder()        
            .name("GitHub-Connection")
            .strategy("github")
            .options(ConnectionOptionsArgs.builder()
                .clientId("\u003cclient-id\u003e")
                .clientSecret("\u003cclient-secret\u003e")
                .scopes(                
                    "email",
                    "profile",
                    "public_repo",
                    "repo")
                .setUserRootAttributes("on_each_login")
                .nonPersistentAttrs(                
                    "ethnicity",
                    "gender")
                .build())
            .build());

    }
}
```
```yaml
resources:
  # This is an example of an GitHub connection.
  github:
    type: auth0:Connection
    properties:
      name: GitHub-Connection
      strategy: github
      options:
        clientId: \u003cclient-id\u003e
        clientSecret: \u003cclient-secret\u003e
        scopes:
          - email
          - profile
          - public_repo
          - repo
        setUserRootAttributes: on_each_login
        nonPersistentAttrs:
          - ethnicity
          - gender
```
\u003c!--End PulumiCodeChooser --\u003e

### SalesForce Connection

\u003c!--Start PulumiCodeChooser --\u003e
```typescript
import * as pulumi from "@pulumi/pulumi";
import * as auth0 from "@pulumi/auth0";

// This is an example of an SalesForce connection.
const salesforce = new auth0.Connection("salesforce", {
    name: "Salesforce-Connection",
    strategy: "salesforce",
    options: {
        clientId: "\u003cclient-id\u003e",
        clientSecret: "\u003cclient-secret\u003e",
        communityBaseUrl: "https://salesforce.example.com",
        scopes: [
            "openid",
            "email",
        ],
        setUserRootAttributes: "on_first_login",
        nonPersistentAttrs: [
            "ethnicity",
            "gender",
        ],
    },
});
```
```python
import pulumi
import pulumi_auth0 as auth0

# This is an example of an SalesForce connection.
salesforce = auth0.Connection("salesforce",
    name="Salesforce-Connection",
    strategy="salesforce",
    options=auth0.ConnectionOptionsArgs(
        client_id="\u003cclient-id\u003e",
        client_secret="\u003cclient-secret\u003e",
        community_base_url="https://salesforce.example.com",
        scopes=[
            "openid",
            "email",
        ],
        set_user_root_attributes="on_first_login",
        non_persistent_attrs=[
            "ethnicity",
            "gender",
        ],
    ))
```
```csharp
using System.Collections.Generic;
using System.Linq;
using Pulumi;
using Auth0 = Pulumi.Auth0;

return await Deployment.RunAsync(() =\u003e 
{
    // This is an example of an SalesForce connection.
    var salesforce = new Auth0.Connection("salesforce", new()
    {
        Name = "Salesforce-Connection",
        Strategy = "salesforce",
        Options = new Auth0.Inputs.ConnectionOptionsArgs
        {
            ClientId = "\u003cclient-id\u003e",
            ClientSecret = "\u003cclient-secret\u003e",
            CommunityBaseUrl = "https://salesforce.example.com",
            Scopes = new[]
            {
                "openid",
                "email",
            },
            SetUserRootAttributes = "on_first_login",
            NonPersistentAttrs = new[]
            {
                "ethnicity",
                "gender",
            },
        },
    });

});
```
```go
package main

import (
	"github.com/pulumi/pulumi-auth0/sdk/v3/go/auth0"
	"github.com/pulumi/pulumi/sdk/v3/go/pulumi"
)

func main() {
	pulumi.Run(func(ctx *pulumi.Context) error {
		// This is an example of an SalesForce connection.
		_, err := auth0.NewConnection(ctx, "salesforce", \u0026auth0.ConnectionArgs{
			Name:     pulumi.String("Salesforce-Connection"),
			Strategy: pulumi.String("salesforce"),
			Options: \u0026auth0.ConnectionOptionsArgs{
				ClientId:         pulumi.String("\u003cclient-id\u003e"),
				ClientSecret:     pulumi.String("\u003cclient-secret\u003e"),
				CommunityBaseUrl: pulumi.String("https://salesforce.example.com"),
				Scopes: pulumi.StringArray{
					pulumi.String("openid"),
					pulumi.String("email"),
				},
				SetUserRootAttributes: pulumi.String("on_first_login"),
				NonPersistentAttrs: pulumi.StringArray{
					pulumi.String("ethnicity"),
					pulumi.String("gender"),
				},
			},
		})
		if err != nil {
			return err
		}
		return nil
	})
}
```
```java
package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.auth0.Connection;
import com.pulumi.auth0.ConnectionArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        // This is an example of an SalesForce connection.
        var salesforce = new Connection("salesforce", ConnectionArgs.builder()        
            .name("Salesforce-Connection")
            .strategy("salesforce")
            .options(ConnectionOptionsArgs.builder()
                .clientId("\u003cclient-id\u003e")
                .clientSecret("\u003cclient-secret\u003e")
                .communityBaseUrl("https://salesforce.example.com")
                .scopes(                
                    "openid",
                    "email")
                .setUserRootAttributes("on_first_login")
                .nonPersistentAttrs(                
                    "ethnicity",
                    "gender")
                .build())
            .build());

    }
}
```
```yaml
resources:
  # This is an example of an SalesForce connection.
  salesforce:
    type: auth0:Connection
    properties:
      name: Salesforce-Connection
      strategy: salesforce
      options:
        clientId: \u003cclient-id\u003e
        clientSecret: \u003cclient-secret\u003e
        communityBaseUrl: https://salesforce.example.com
        scopes:
          - openid
          - email
        setUserRootAttributes: on_first_login
        nonPersistentAttrs:
          - ethnicity
          - gender
```
\u003c!--End PulumiCodeChooser --\u003e

### OAuth2 Connection

Also applies to following connection strategies: `dropbox`, `bitbucket`, `paypal`, `twitter`, `amazon`, `yahoo`, `box`, `wordpress`, `shopify`, `custom`

\u003c!--Start PulumiCodeChooser --\u003e
```typescript
import * as pulumi from "@pulumi/pulumi";
import * as auth0 from "@pulumi/auth0";

// This is an example of an OAuth2 connection.
const oauth2 = new auth0.Connection("oauth2", {
    name: "OAuth2-Connection",
    strategy: "oauth2",
    options: {
        clientId: "\u003cclient-id\u003e",
        clientSecret: "\u003cclient-secret\u003e",
        scopes: [
            "basic_profile",
            "profile",
            "email",
        ],
        tokenEndpoint: "https://auth.example.com/oauth2/token",
        authorizationEndpoint: "https://auth.example.com/oauth2/authorize",
        pkceEnabled: true,
        iconUrl: "https://auth.example.com/assets/logo.png",
        scripts: {
            fetchUserProfile: `        function fetchUserProfile(accessToken, context, callback) {
          return callback(new Error("Whoops!"));
        }
`,
        },
        setUserRootAttributes: "on_each_login",
        nonPersistentAttrs: [
            "ethnicity",
            "gender",
        ],
    },
});
```
```python
import pulumi
import pulumi_auth0 as auth0

# This is an example of an OAuth2 connection.
oauth2 = auth0.Connection("oauth2",
    name="OAuth2-Connection",
    strategy="oauth2",
    options=auth0.ConnectionOptionsArgs(
        client_id="\u003cclient-id\u003e",
        client_secret="\u003cclient-secret\u003e",
        scopes=[
            "basic_profile",
            "profile",
            "email",
        ],
        token_endpoint="https://auth.example.com/oauth2/token",
        authorization_endpoint="https://auth.example.com/oauth2/authorize",
        pkce_enabled=True,
        icon_url="https://auth.example.com/assets/logo.png",
        scripts={
            "fetchUserProfile": """        function fetchUserProfile(accessToken, context, callback) {
          return callback(new Error("Whoops!"));
        }
""",
        },
        set_user_root_attributes="on_each_login",
        non_persistent_attrs=[
            "ethnicity",
            "gender",
        ],
    ))
```
```csharp
using System.Collections.Generic;
using System.Linq;
using Pulumi;
using Auth0 = Pulumi.Auth0;

return await Deployment.RunAsync(() =\u003e 
{
    // This is an example of an OAuth2 connection.
    var oauth2 = new Auth0.Connection("oauth2", new()
    {
        Name = "OAuth2-Connection",
        Strategy = "oauth2",
        Options = new Auth0.Inputs.ConnectionOptionsArgs
        {
            ClientId = "\u003cclient-id\u003e",
            ClientSecret = "\u003cclient-secret\u003e",
            Scopes = new[]
            {
                "basic_profile",
                "profile",
                "email",
            },
            TokenEndpoint = "https://auth.example.com/oauth2/token",
            AuthorizationEndpoint = "https://auth.example.com/oauth2/authorize",
            PkceEnabled = true,
            IconUrl = "https://auth.example.com/assets/logo.png",
            Scripts = 
            {
                { "fetchUserProfile", @"        function fetchUserProfile(accessToken, context, callback) {
          return callback(new Error(""Whoops!""));
        }
" },
            },
            SetUserRootAttributes = "on_each_login",
            NonPersistentAttrs = new[]
            {
                "ethnicity",
                "gender",
            },
        },
    });

});
```
```go
package main

import (
	"github.com/pulumi/pulumi-auth0/sdk/v3/go/auth0"
	"github.com/pulumi/pulumi/sdk/v3/go/pulumi"
)

func main() {
	pulumi.Run(func(ctx *pulumi.Context) error {
		// This is an example of an OAuth2 connection.
		_, err := auth0.NewConnection(ctx, "oauth2", \u0026auth0.ConnectionArgs{
			Name:     pulumi.String("OAuth2-Connection"),
			Strategy: pulumi.String("oauth2"),
			Options: \u0026auth0.ConnectionOptionsArgs{
				ClientId:     pulumi.String("\u003cclient-id\u003e"),
				ClientSecret: pulumi.String("\u003cclient-secret\u003e"),
				Scopes: pulumi.StringArray{
					pulumi.String("basic_profile"),
					pulumi.String("profile"),
					pulumi.String("email"),
				},
				TokenEndpoint:         pulumi.String("https://auth.example.com/oauth2/token"),
				AuthorizationEndpoint: pulumi.String("https://auth.example.com/oauth2/authorize"),
				PkceEnabled:           pulumi.Bool(true),
				IconUrl:               pulumi.String("https://auth.example.com/assets/logo.png"),
				Scripts: pulumi.StringMap{
					"fetchUserProfile": pulumi.String("        function fetchUserProfile(accessToken, context, callback) {\
          return callback(new Error(\\"Whoops!\\"));\
        }\
"),
				},
				SetUserRootAttributes: pulumi.String("on_each_login"),
				NonPersistentAttrs: pulumi.StringArray{
					pulumi.String("ethnicity"),
					pulumi.String("gender"),
				},
			},
		})
		if err != nil {
			return err
		}
		return nil
	})
}
```
```java
package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.auth0.Connection;
import com.pulumi.auth0.ConnectionArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        // This is an example of an OAuth2 connection.
        var oauth2 = new Connection("oauth2", ConnectionArgs.builder()        
            .name("OAuth2-Connection")
            .strategy("oauth2")
            .options(ConnectionOptionsArgs.builder()
                .clientId("\u003cclient-id\u003e")
                .clientSecret("\u003cclient-secret\u003e")
                .scopes(                
                    "basic_profile",
                    "profile",
                    "email")
                .tokenEndpoint("https://auth.example.com/oauth2/token")
                .authorizationEndpoint("https://auth.example.com/oauth2/authorize")
                .pkceEnabled(true)
                .iconUrl("https://auth.example.com/assets/logo.png")
                .scripts(Map.of("fetchUserProfile", """
        function fetchUserProfile(accessToken, context, callback) {
          return callback(new Error("Whoops!"));
        }
                """))
                .setUserRootAttributes("on_each_login")
                .nonPersistentAttrs(                
                    "ethnicity",
                    "gender")
                .build())
            .build());

    }
}
```
```yaml
resources:
  # This is an example of an OAuth2 connection.
  oauth2:
    type: auth0:Connection
    properties:
      name: OAuth2-Connection
      strategy: oauth2
      options:
        clientId: \u003cclient-id\u003e
        clientSecret: \u003cclient-secret\u003e
        scopes:
          - basic_profile
          - profile
          - email
        tokenEndpoint: https://auth.example.com/oauth2/token
        authorizationEndpoint: https://auth.example.com/oauth2/authorize
        pkceEnabled: true
        iconUrl: https://auth.example.com/assets/logo.png
        scripts:
          fetchUserProfile: |2
                    function fetchUserProfile(accessToken, context, callback) {
                      return callback(new Error("Whoops!"));
                    }
        setUserRootAttributes: on_each_login
        nonPersistentAttrs:
          - ethnicity
          - gender
```
\u003c!--End PulumiCodeChooser --\u003e

### Active Directory (AD)

\u003c!--Start PulumiCodeChooser --\u003e
```typescript
import * as pulumi from "@pulumi/pulumi";
import * as auth0 from "@pulumi/auth0";

const ad = new auth0.Connection("ad", {
    name: "connection-active-directory",
    displayName: "Active Directory Connection",
    strategy: "ad",
    showAsButton: true,
    options: {
        disableSelfServiceChangePassword: true,
        bruteForceProtection: true,
        tenantDomain: "example.com",
        iconUrl: "https://example.com/assets/logo.png",
        domainAliases: [
            "example.com",
            "api.example.com",
        ],
        ips: [
            "192.168.1.1",
            "192.168.1.2",
        ],
        setUserRootAttributes: "on_each_login",
        nonPersistentAttrs: [
            "ethnicity",
            "gender",
        ],
        upstreamParams: JSON.stringify({
            screen_name: {
                alias: "login_hint",
            },
        }),
        useCertAuth: false,
        useKerberos: false,
        disableCache: false,
    },
});
```
```python
import pulumi
import json
import pulumi_auth0 as auth0

ad = auth0.Connection("ad",
    name="connection-active-directory",
    display_name="Active Directory Connection",
    strategy="ad",
    show_as_button=True,
    options=auth0.ConnectionOptionsArgs(
        disable_self_service_change_password=True,
        brute_force_protection=True,
        tenant_domain="example.com",
        icon_url="https://example.com/assets/logo.png",
        domain_aliases=[
            "example.com",
            "api.example.com",
        ],
        ips=[
            "192.168.1.1",
            "192.168.1.2",
        ],
        set_user_root_attributes="on_each_login",
        non_persistent_attrs=[
            "ethnicity",
            "gender",
        ],
        upstream_params=json.dumps({
            "screen_name": {
                "alias": "login_hint",
            },
        }),
        use_cert_auth=False,
        use_kerberos=False,
        disable_cache=False,
    ))
```
```csharp
using System.Collections.Generic;
using System.Linq;
using System.Text.Json;
using Pulumi;
using Auth0 = Pulumi.Auth0;

return await Deployment.RunAsync(() =\u003e 
{
    var ad = new Auth0.Connection("ad", new()
    {
        Name = "connection-active-directory",
        DisplayName = "Active Directory Connection",
        Strategy = "ad",
        ShowAsButton = true,
        Options = new Auth0.Inputs.ConnectionOptionsArgs
        {
            DisableSelfServiceChangePassword = true,
            BruteForceProtection = true,
            TenantDomain = "example.com",
            IconUrl = "https://example.com/assets/logo.png",
            DomainAliases = new[]
            {
                "example.com",
                "api.example.com",
            },
            Ips = new[]
            {
                "192.168.1.1",
                "192.168.1.2",
            },
            SetUserRootAttributes = "on_each_login",
            NonPersistentAttrs = new[]
            {
                "ethnicity",
                "gender",
            },
            UpstreamParams = JsonSerializer.Serialize(new Dictionary\u003cstring, object?\u003e
            {
                ["screen_name"] = new Dictionary\u003cstring, object?\u003e
                {
                    ["alias"] = "login_hint",
                },
            }),
            UseCertAuth = false,
            UseKerberos = false,
            DisableCache = false,
        },
    });

});
```
```go
package main

import (
	"encoding/json"

	"github.com/pulumi/pulumi-auth0/sdk/v3/go/auth0"
	"github.com/pulumi/pulumi/sdk/v3/go/pulumi"
)

func main() {
	pulumi.Run(func(ctx *pulumi.Context) error {
		tmpJSON0, err := json.Marshal(map[string]interface{}{
			"screen_name": map[string]interface{}{
				"alias": "login_hint",
			},
		})
		if err != nil {
			return err
		}
		json0 := string(tmpJSON0)
		_, err = auth0.NewConnection(ctx, "ad", \u0026auth0.ConnectionArgs{
			Name:         pulumi.String("connection-active-directory"),
			DisplayName:  pulumi.String("Active Directory Connection"),
			Strategy:     pulumi.String("ad"),
			ShowAsButton: pulumi.Bool(true),
			Options: \u0026auth0.ConnectionOptionsArgs{
				DisableSelfServiceChangePassword: pulumi.Bool(true),
				BruteForceProtection:             pulumi.Bool(true),
				TenantDomain:                     pulumi.String("example.com"),
				IconUrl:                          pulumi.String("https://example.com/assets/logo.png"),
				DomainAliases: pulumi.StringArray{
					pulumi.String("example.com"),
					pulumi.String("api.example.com"),
				},
				Ips: pulumi.StringArray{
					pulumi.String("192.168.1.1"),
					pulumi.String("192.168.1.2"),
				},
				SetUserRootAttributes: pulumi.String("on_each_login"),
				NonPersistentAttrs: pulumi.StringArray{
					pulumi.String("ethnicity"),
					pulumi.String("gender"),
				},
				UpstreamParams: pulumi.String(json0),
				UseCertAuth:    pulumi.Bool(false),
				UseKerberos:    pulumi.Bool(false),
				DisableCache:   pulumi.Bool(false),
			},
		})
		if err != nil {
			return err
		}
		return nil
	})
}
```
```java
package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.auth0.Connection;
import com.pulumi.auth0.ConnectionArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
import static com.pulumi.codegen.internal.Serialization.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        var ad = new Connection("ad", ConnectionArgs.builder()        
            .name("connection-active-directory")
            .displayName("Active Directory Connection")
            .strategy("ad")
            .showAsButton(true)
            .options(ConnectionOptionsArgs.builder()
                .disableSelfServiceChangePassword(true)
                .bruteForceProtection(true)
                .tenantDomain("example.com")
                .iconUrl("https://example.com/assets/logo.png")
                .domainAliases(                
                    "example.com",
                    "api.example.com")
                .ips(                
                    "192.168.1.1",
                    "192.168.1.2")
                .setUserRootAttributes("on_each_login")
                .nonPersistentAttrs(                
                    "ethnicity",
                    "gender")
                .upstreamParams(serializeJson(
                    jsonObject(
                        jsonProperty("screen_name", jsonObject(
                            jsonProperty("alias", "login_hint")
                        ))
                    )))
                .useCertAuth(false)
                .useKerberos(false)
                .disableCache(false)
                .build())
            .build());

    }
}
```
```yaml
resources:
  ad:
    type: auth0:Connection
    properties:
      name: connection-active-directory
      displayName: Active Directory Connection
      strategy: ad
      showAsButton: true
      options:
        disableSelfServiceChangePassword: true
        bruteForceProtection: true
        tenantDomain: example.com
        iconUrl: https://example.com/assets/logo.png
        domainAliases:
          - example.com
          - api.example.com
        ips:
          - 192.168.1.1
          - 192.168.1.2
        setUserRootAttributes: on_each_login
        nonPersistentAttrs:
          - ethnicity
          - gender
        upstreamParams:
          fn::toJSON:
            screen_name:
              alias: login_hint
        useCertAuth: false
        useKerberos: false
        disableCache: false
```
\u003c!--End PulumiCodeChooser --\u003e

### Azure AD Connection

\u003c!--Start PulumiCodeChooser --\u003e
```typescript
import * as pulumi from "@pulumi/pulumi";
import * as auth0 from "@pulumi/auth0";

const azureAd = new auth0.Connection("azure_ad", {
    name: "connection-azure-ad",
    strategy: "waad",
    showAsButton: true,
    options: {
        identityApi: "azure-active-directory-v1.0",
        clientId: "123456",
        clientSecret: "123456",
        appId: "app-id-123",
        tenantDomain: "example.onmicrosoft.com",
        domain: "example.onmicrosoft.com",
        domainAliases: [
            "example.com",
            "api.example.com",
        ],
        iconUrl: "https://example.onmicrosoft.com/assets/logo.png",
        useWsfed: false,
        waadProtocol: "openid-connect",
        waadCommonEndpoint: false,
        maxGroupsToRetrieve: "250",
        apiEnableUsers: true,
        scopes: [
            "basic_profile",
            "ext_groups",
            "ext_profile",
        ],
        setUserRootAttributes: "on_each_login",
        shouldTrustEmailVerifiedConnection: "never_set_emails_as_verified",
        upstreamParams: JSON.stringify({
            screen_name: {
                alias: "login_hint",
            },
        }),
        nonPersistentAttrs: [
            "ethnicity",
            "gender",
        ],
    },
});
```
```python
import pulumi
import json
import pulumi_auth0 as auth0

azure_ad = auth0.Connection("azure_ad",
    name="connection-azure-ad",
    strategy="waad",
    show_as_button=True,
    options=auth0.ConnectionOptionsArgs(
        identity_api="azure-active-directory-v1.0",
        client_id="123456",
        client_secret="123456",
        app_id="app-id-123",
        tenant_domain="example.onmicrosoft.com",
        domain="example.onmicrosoft.com",
        domain_aliases=[
            "example.com",
            "api.example.com",
        ],
        icon_url="https://example.onmicrosoft.com/assets/logo.png",
        use_wsfed=False,
        waad_protocol="openid-connect",
        waad_common_endpoint=False,
        max_groups_to_retrieve="250",
        api_enable_users=True,
        scopes=[
            "basic_profile",
            "ext_groups",
            "ext_profile",
        ],
        set_user_root_attributes="on_each_login",
        should_trust_email_verified_connection="never_set_emails_as_verified",
        upstream_params=json.dumps({
            "screen_name": {
                "alias": "login_hint",
            },
        }),
        non_persistent_attrs=[
            "ethnicity",
            "gender",
        ],
    ))
```
```csharp
using System.Collections.Generic;
using System.Linq;
using System.Text.Json;
using Pulumi;
using Auth0 = Pulumi.Auth0;

return await Deployment.RunAsync(() =\u003e 
{
    var azureAd = new Auth0.Connection("azure_ad", new()
    {
        Name = "connection-azure-ad",
        Strategy = "waad",
        ShowAsButton = true,
        Options = new Auth0.Inputs.ConnectionOptionsArgs
        {
            IdentityApi = "azure-active-directory-v1.0",
            ClientId = "123456",
            ClientSecret = "123456",
            AppId = "app-id-123",
            TenantDomain = "example.onmicrosoft.com",
            Domain = "example.onmicrosoft.com",
            DomainAliases = new[]
            {
                "example.com",
                "api.example.com",
            },
            IconUrl = "https://example.onmicrosoft.com/assets/logo.png",
            UseWsfed = false,
            WaadProtocol = "openid-connect",
            WaadCommonEndpoint = false,
            MaxGroupsToRetrieve = "250",
            ApiEnableUsers = true,
            Scopes = new[]
            {
                "basic_profile",
                "ext_groups",
                "ext_profile",
            },
            SetUserRootAttributes = "on_each_login",
            ShouldTrustEmailVerifiedConnection = "never_set_emails_as_verified",
            UpstreamParams = JsonSerializer.Serialize(new Dictionary\u003cstring, object?\u003e
            {
                ["screen_name"] = new Dictionary\u003cstring, object?\u003e
                {
                    ["alias"] = "login_hint",
                },
            }),
            NonPersistentAttrs = new[]
            {
                "ethnicity",
                "gender",
            },
        },
    });

});
```
```go
package main

import (
	"encoding/json"

	"github.com/pulumi/pulumi-auth0/sdk/v3/go/auth0"
	"github.com/pulumi/pulumi/sdk/v3/go/pulumi"
)

func main() {
	pulumi.Run(func(ctx *pulumi.Context) error {
		tmpJSON0, err := json.Marshal(map[string]interface{}{
			"screen_name": map[string]interface{}{
				"alias": "login_hint",
			},
		})
		if err != nil {
			return err
		}
		json0 := string(tmpJSON0)
		_, err = auth0.NewConnection(ctx, "azure_ad", \u0026auth0.ConnectionArgs{
			Name:         pulumi.String("connection-azure-ad"),
			Strategy:     pulumi.String("waad"),
			ShowAsButton: pulumi.Bool(true),
			Options: \u0026auth0.ConnectionOptionsArgs{
				IdentityApi:  pulumi.String("azure-active-directory-v1.0"),
				ClientId:     pulumi.String("123456"),
				ClientSecret: pulumi.String("123456"),
				AppId:        pulumi.String("app-id-123"),
				TenantDomain: pulumi.String("example.onmicrosoft.com"),
				Domain:       pulumi.String("example.onmicrosoft.com"),
				DomainAliases: pulumi.StringArray{
					pulumi.String("example.com"),
					pulumi.String("api.example.com"),
				},
				IconUrl:             pulumi.String("https://example.onmicrosoft.com/assets/logo.png"),
				UseWsfed:            pulumi.Bool(false),
				WaadProtocol:        pulumi.String("openid-connect"),
				WaadCommonEndpoint:  pulumi.Bool(false),
				MaxGroupsToRetrieve: pulumi.String("250"),
				ApiEnableUsers:      pulumi.Bool(true),
				Scopes: pulumi.StringArray{
					pulumi.String("basic_profile"),
					pulumi.String("ext_groups"),
					pulumi.String("ext_profile"),
				},
				SetUserRootAttributes:              pulumi.String("on_each_login"),
				ShouldTrustEmailVerifiedConnection: pulumi.String("never_set_emails_as_verified"),
				UpstreamParams:                     pulumi.String(json0),
				NonPersistentAttrs: pulumi.StringArray{
					pulumi.String("ethnicity"),
					pulumi.String("gender"),
				},
			},
		})
		if err != nil {
			return err
		}
		return nil
	})
}
```
```java
package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.auth0.Connection;
import com.pulumi.auth0.ConnectionArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
import static com.pulumi.codegen.internal.Serialization.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        var azureAd = new Connection("azureAd", ConnectionArgs.builder()        
            .name("connection-azure-ad")
            .strategy("waad")
            .showAsButton(true)
            .options(ConnectionOptionsArgs.builder()
                .identityApi("azure-active-directory-v1.0")
                .clientId("123456")
                .clientSecret("123456")
                .appId("app-id-123")
                .tenantDomain("example.onmicrosoft.com")
                .domain("example.onmicrosoft.com")
                .domainAliases(                
                    "example.com",
                    "api.example.com")
                .iconUrl("https://example.onmicrosoft.com/assets/logo.png")
                .useWsfed(false)
                .waadProtocol("openid-connect")
                .waadCommonEndpoint(false)
                .maxGroupsToRetrieve(250)
                .apiEnableUsers(true)
                .scopes(                
                    "basic_profile",
                    "ext_groups",
                    "ext_profile")
                .setUserRootAttributes("on_each_login")
                .shouldTrustEmailVerifiedConnection("never_set_emails_as_verified")
                .upstreamParams(serializeJson(
                    jsonObject(
                        jsonProperty("screen_name", jsonObject(
                            jsonProperty("alias", "login_hint")
                        ))
                    )))
                .nonPersistentAttrs(                
                    "ethnicity",
                    "gender")
                .build())
            .build());

    }
}
```
```yaml
resources:
  azureAd:
    type: auth0:Connection
    name: azure_ad
    properties:
      name: connection-azure-ad
      strategy: waad
      showAsButton: true
      options:
        identityApi: azure-active-directory-v1.0
        clientId: '123456'
        clientSecret: '123456'
        appId: app-id-123
        tenantDomain: example.onmicrosoft.com
        domain: example.onmicrosoft.com
        domainAliases:
          - example.com
          - api.example.com
        iconUrl: https://example.onmicrosoft.com/assets/logo.png
        useWsfed: false
        waadProtocol: openid-connect
        waadCommonEndpoint: false
        maxGroupsToRetrieve: 250
        apiEnableUsers: true
        scopes:
          - basic_profile
          - ext_groups
          - ext_profile
        setUserRootAttributes: on_each_login
        shouldTrustEmailVerifiedConnection: never_set_emails_as_verified
        upstreamParams:
          fn::toJSON:
            screen_name:
              alias: login_hint
        nonPersistentAttrs:
          - ethnicity
          - gender
```
\u003c!--End PulumiCodeChooser --\u003e

### Email Connection

\u003e To be able to see this in the management dashboard as well, the name of the connection must be set to "email".

\u003c!--Start PulumiCodeChooser --\u003e
```typescript
import * as pulumi from "@pulumi/pulumi";
import * as auth0 from "@pulumi/auth0";

// This is an example of an Email connection.
const passwordlessEmail = new auth0.Connection("passwordless_email", {
    strategy: "email",
    name: "email",
    options: {
        name: "email",
        from: "{{ application.name }} \u003croot@auth0.com\u003e",
        subject: "Welcome to {{ application.name }}",
        syntax: "liquid",
        template: "\u003chtml\u003eThis is the body of the email\u003c/html\u003e",
        disableSignup: false,
        bruteForceProtection: true,
        setUserRootAttributes: "on_each_login",
        nonPersistentAttrs: [],
        authParams: {
            scope: "openid email profile offline_access",
            response_type: "code",
        },
        totp: {
            timeStep: 300,
            length: 6,
        },
    },
});
```
```python
import pulumi
import pulumi_auth0 as auth0

# This is an example of an Email connection.
passwordless_email = auth0.Connection("passwordless_email",
    strategy="email",
    name="email",
    options=auth0.ConnectionOptionsArgs(
        name="email",
        from_="{{ application.name }} \u003croot@auth0.com\u003e",
        subject="Welcome to {{ application.name }}",
        syntax="liquid",
        template="\u003chtml\u003eThis is the body of the email\u003c/html\u003e",
        disable_signup=False,
        brute_force_protection=True,
        set_user_root_attributes="on_each_login",
        non_persistent_attrs=[],
        auth_params={
            "scope": "openid email profile offline_access",
            "response_type": "code",
        },
        totp=auth0.ConnectionOptionsTotpArgs(
            time_step=300,
            length=6,
        ),
    ))
```
```csharp
using System.Collections.Generic;
using System.Linq;
using Pulumi;
using Auth0 = Pulumi.Auth0;

return await Deployment.RunAsync(() =\u003e 
{
    // This is an example of an Email connection.
    var passwordlessEmail = new Auth0.Connection("passwordless_email", new()
    {
        Strategy = "email",
        Name = "email",
        Options = new Auth0.Inputs.ConnectionOptionsArgs
        {
            Name = "email",
            From = "{{ application.name }} \u003croot@auth0.com\u003e",
            Subject = "Welcome to {{ application.name }}",
            Syntax = "liquid",
            Template = "\u003chtml\u003eThis is the body of the email\u003c/html\u003e",
            DisableSignup = false,
            BruteForceProtection = true,
            SetUserRootAttributes = "on_each_login",
            NonPersistentAttrs = new() { },
            AuthParams = 
            {
                { "scope", "openid email profile offline_access" },
                { "response_type", "code" },
            },
            Totp = new Auth0.Inputs.ConnectionOptionsTotpArgs
            {
                TimeStep = 300,
                Length = 6,
            },
        },
    });

});
```
```go
package main

import (
	"github.com/pulumi/pulumi-auth0/sdk/v3/go/auth0"
	"github.com/pulumi/pulumi/sdk/v3/go/pulumi"
)

func main() {
	pulumi.Run(func(ctx *pulumi.Context) error {
		// This is an example of an Email connection.
		_, err := auth0.NewConnection(ctx, "passwordless_email", \u0026auth0.ConnectionArgs{
			Strategy: pulumi.String("email"),
			Name:     pulumi.String("email"),
			Options: \u0026auth0.ConnectionOptionsArgs{
				Name:                  pulumi.String("email"),
				From:                  pulumi.String("{{ application.name }} \u003croot@auth0.com\u003e"),
				Subject:               pulumi.String("Welcome to {{ application.name }}"),
				Syntax:                pulumi.String("liquid"),
				Template:              pulumi.String("\u003chtml\u003eThis is the body of the email\u003c/html\u003e"),
				DisableSignup:         pulumi.Bool(false),
				BruteForceProtection:  pulumi.Bool(true),
				SetUserRootAttributes: pulumi.String("on_each_login"),
				NonPersistentAttrs:    pulumi.StringArray{},
				AuthParams: pulumi.StringMap{
					"scope":         pulumi.String("openid email profile offline_access"),
					"response_type": pulumi.String("code"),
				},
				Totp: \u0026auth0.ConnectionOptionsTotpArgs{
					TimeStep: pulumi.Int(300),
					Length:   pulumi.Int(6),
				},
			},
		})
		if err != nil {
			return err
		}
		return nil
	})
}
```
```java
package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.auth0.Connection;
import com.pulumi.auth0.ConnectionArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsTotpArgs;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        // This is an example of an Email connection.
        var passwordlessEmail = new Connection("passwordlessEmail", ConnectionArgs.builder()        
            .strategy("email")
            .name("email")
            .options(ConnectionOptionsArgs.builder()
                .name("email")
                .from("{{ application.name }} \u003croot@auth0.com\u003e")
                .subject("Welcome to {{ application.name }}")
                .syntax("liquid")
                .template("\u003chtml\u003eThis is the body of the email\u003c/html\u003e")
                .disableSignup(false)
                .bruteForceProtection(true)
                .setUserRootAttributes("on_each_login")
                .nonPersistentAttrs()
                .authParams(Map.ofEntries(
                    Map.entry("scope", "openid email profile offline_access"),
                    Map.entry("response_type", "code")
                ))
                .totp(ConnectionOptionsTotpArgs.builder()
                    .timeStep(300)
                    .length(6)
                    .build())
                .build())
            .build());

    }
}
```
```yaml
resources:
  # This is an example of an Email connection.
  passwordlessEmail:
    type: auth0:Connection
    name: passwordless_email
    properties:
      strategy: email
      name: email
      options:
        name: email
        from: '{{ application.name }} \u003croot@auth0.com\u003e'
        subject: Welcome to {{ application.name }}
        syntax: liquid
        template: \u003chtml\u003eThis is the body of the email\u003c/html\u003e
        disableSignup: false
        bruteForceProtection: true
        setUserRootAttributes: on_each_login
        nonPersistentAttrs: []
        authParams:
          scope: openid email profile offline_access
          response_type: code
        totp:
          timeStep: 300
          length: 6
```
\u003c!--End PulumiCodeChooser --\u003e

### SAML Connection

\u003c!--Start PulumiCodeChooser --\u003e
```typescript
import * as pulumi from "@pulumi/pulumi";
import * as auth0 from "@pulumi/auth0";

// This is an example of a SAML connection.
const samlp = new auth0.Connection("samlp", {
    name: "SAML-Connection",
    strategy: "samlp",
    options: {
        debug: false,
        signingCert: "\u003csigning-certificate\u003e",
        signInEndpoint: "https://saml.provider/sign_in",
        signOutEndpoint: "https://saml.provider/sign_out",
        disableSignOut: true,
        tenantDomain: "example.com",
        domainAliases: [
            "example.com",
            "alias.example.com",
        ],
        protocolBinding: "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST",
        requestTemplate: `\u003csamlp:AuthnRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
@@AssertServiceURLAndDestination@@
    ID="@@ID@@"
    IssueInstant="@@IssueInstant@@"
    ProtocolBinding="@@ProtocolBinding@@" Version="2.0"\u003e
    \u003csaml:Issuer xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"\u003e@@Issuer@@\u003c/saml:Issuer\u003e
\u003c/samlp:AuthnRequest\u003e`,
        userIdAttribute: "https://saml.provider/imi/ns/identity-200810",
        signatureAlgorithm: "rsa-sha256",
        digestAlgorithm: "sha256",
        iconUrl: "https://saml.provider/assets/logo.png",
        entityId: "\u003centity_id\u003e",
        metadataXml: `    \u003c?xml version="1.0"?\u003e
    \u003cmd:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" xmlns:ds="http://www.w3.org/2000/09/xmldsig#" entityID="https://example.com"\u003e
      \u003cmd:IDPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol"\u003e
        \u003cmd:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://saml.provider/sign_out"/\u003e
        \u003cmd:SingleSignOnService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://saml.provider/sign_in"/\u003e
      \u003c/md:IDPSSODescriptor\u003e
    \u003c/md:EntityDescriptor\u003e
`,
        metadataUrl: "https://saml.provider/imi/ns/FederationMetadata.xml",
        fieldsMap: JSON.stringify({
            name: [
                "name",
                "nameidentifier",
            ],
            email: [
                "emailaddress",
                "nameidentifier",
            ],
            family_name: "surname",
        }),
        signingKey: {
            key: `-----BEGIN PRIVATE KEY-----
...{your private key here}...
-----END PRIVATE KEY-----`,
            cert: `-----BEGIN CERTIFICATE-----
...{your public key cert here}...
-----END CERTIFICATE-----`,
        },
        decryptionKey: {
            key: `-----BEGIN PRIVATE KEY-----
...{your private key here}...
-----END PRIVATE KEY-----`,
            cert: `-----BEGIN CERTIFICATE-----
...{your public key cert here}...
-----END CERTIFICATE-----`,
        },
        idpInitiated: {
            clientId: "client_id",
            clientProtocol: "samlp",
            clientAuthorizeQuery: "type=code\u0026timeout=30",
        },
    },
});
```
```python
import pulumi
import json
import pulumi_auth0 as auth0

# This is an example of a SAML connection.
samlp = auth0.Connection("samlp",
    name="SAML-Connection",
    strategy="samlp",
    options=auth0.ConnectionOptionsArgs(
        debug=False,
        signing_cert="\u003csigning-certificate\u003e",
        sign_in_endpoint="https://saml.provider/sign_in",
        sign_out_endpoint="https://saml.provider/sign_out",
        disable_sign_out=True,
        tenant_domain="example.com",
        domain_aliases=[
            "example.com",
            "alias.example.com",
        ],
        protocol_binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST",
        request_template="""\u003csamlp:AuthnRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
@@AssertServiceURLAndDestination@@
    ID="@@ID@@"
    IssueInstant="@@IssueInstant@@"
    ProtocolBinding="@@ProtocolBinding@@" Version="2.0"\u003e
    \u003csaml:Issuer xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"\u003e@@Issuer@@\u003c/saml:Issuer\u003e
\u003c/samlp:AuthnRequest\u003e""",
        user_id_attribute="https://saml.provider/imi/ns/identity-200810",
        signature_algorithm="rsa-sha256",
        digest_algorithm="sha256",
        icon_url="https://saml.provider/assets/logo.png",
        entity_id="\u003centity_id\u003e",
        metadata_xml="""    \u003c?xml version="1.0"?\u003e
    \u003cmd:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" xmlns:ds="http://www.w3.org/2000/09/xmldsig#" entityID="https://example.com"\u003e
      \u003cmd:IDPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol"\u003e
        \u003cmd:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://saml.provider/sign_out"/\u003e
        \u003cmd:SingleSignOnService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://saml.provider/sign_in"/\u003e
      \u003c/md:IDPSSODescriptor\u003e
    \u003c/md:EntityDescriptor\u003e
""",
        metadata_url="https://saml.provider/imi/ns/FederationMetadata.xml",
        fields_map=json.dumps({
            "name": [
                "name",
                "nameidentifier",
            ],
            "email": [
                "emailaddress",
                "nameidentifier",
            ],
            "family_name": "surname",
        }),
        signing_key=auth0.ConnectionOptionsSigningKeyArgs(
            key="""-----BEGIN PRIVATE KEY-----
...{your private key here}...
-----END PRIVATE KEY-----""",
            cert="""-----BEGIN CERTIFICATE-----
...{your public key cert here}...
-----END CERTIFICATE-----""",
        ),
        decryption_key=auth0.ConnectionOptionsDecryptionKeyArgs(
            key="""-----BEGIN PRIVATE KEY-----
...{your private key here}...
-----END PRIVATE KEY-----""",
            cert="""-----BEGIN CERTIFICATE-----
...{your public key cert here}...
-----END CERTIFICATE-----""",
        ),
        idp_initiated=auth0.ConnectionOptionsIdpInitiatedArgs(
            client_id="client_id",
            client_protocol="samlp",
            client_authorize_query="type=code\u0026timeout=30",
        ),
    ))
```
```csharp
using System.Collections.Generic;
using System.Linq;
using System.Text.Json;
using Pulumi;
using Auth0 = Pulumi.Auth0;

return await Deployment.RunAsync(() =\u003e 
{
    // This is an example of a SAML connection.
    var samlp = new Auth0.Connection("samlp", new()
    {
        Name = "SAML-Connection",
        Strategy = "samlp",
        Options = new Auth0.Inputs.ConnectionOptionsArgs
        {
            Debug = false,
            SigningCert = "\u003csigning-certificate\u003e",
            SignInEndpoint = "https://saml.provider/sign_in",
            SignOutEndpoint = "https://saml.provider/sign_out",
            DisableSignOut = true,
            TenantDomain = "example.com",
            DomainAliases = new[]
            {
                "example.com",
                "alias.example.com",
            },
            ProtocolBinding = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST",
            RequestTemplate = @"\u003csamlp:AuthnRequest xmlns:samlp=""urn:oasis:names:tc:SAML:2.0:protocol""
@@AssertServiceURLAndDestination@@
    ID=""@@ID@@""
    IssueInstant=""@@IssueInstant@@""
    ProtocolBinding=""@@ProtocolBinding@@"" Version=""2.0""\u003e
    \u003csaml:Issuer xmlns:saml=""urn:oasis:names:tc:SAML:2.0:assertion""\u003e@@Issuer@@\u003c/saml:Issuer\u003e
\u003c/samlp:AuthnRequest\u003e",
            UserIdAttribute = "https://saml.provider/imi/ns/identity-200810",
            SignatureAlgorithm = "rsa-sha256",
            DigestAlgorithm = "sha256",
            IconUrl = "https://saml.provider/assets/logo.png",
            EntityId = "\u003centity_id\u003e",
            MetadataXml = @"    \u003c?xml version=""1.0""?\u003e
    \u003cmd:EntityDescriptor xmlns:md=""urn:oasis:names:tc:SAML:2.0:metadata"" xmlns:ds=""http://www.w3.org/2000/09/xmldsig#"" entityID=""https://example.com""\u003e
      \u003cmd:IDPSSODescriptor protocolSupportEnumeration=""urn:oasis:names:tc:SAML:2.0:protocol""\u003e
        \u003cmd:SingleLogoutService Binding=""urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect"" Location=""https://saml.provider/sign_out""/\u003e
        \u003cmd:SingleSignOnService Binding=""urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect"" Location=""https://saml.provider/sign_in""/\u003e
      \u003c/md:IDPSSODescriptor\u003e
    \u003c/md:EntityDescriptor\u003e
",
            MetadataUrl = "https://saml.provider/imi/ns/FederationMetadata.xml",
            FieldsMap = JsonSerializer.Serialize(new Dictionary\u003cstring, object?\u003e
            {
                ["name"] = new[]
                {
                    "name",
                    "nameidentifier",
                },
                ["email"] = new[]
                {
                    "emailaddress",
                    "nameidentifier",
                },
                ["family_name"] = "surname",
            }),
            SigningKey = new Auth0.Inputs.ConnectionOptionsSigningKeyArgs
            {
                Key = @"-----BEGIN PRIVATE KEY-----
...{your private key here}...
-----END PRIVATE KEY-----",
                Cert = @"-----BEGIN CERTIFICATE-----
...{your public key cert here}...
-----END CERTIFICATE-----",
            },
            DecryptionKey = new Auth0.Inputs.ConnectionOptionsDecryptionKeyArgs
            {
                Key = @"-----BEGIN PRIVATE KEY-----
...{your private key here}...
-----END PRIVATE KEY-----",
                Cert = @"-----BEGIN CERTIFICATE-----
...{your public key cert here}...
-----END CERTIFICATE-----",
            },
            IdpInitiated = new Auth0.Inputs.ConnectionOptionsIdpInitiatedArgs
            {
                ClientId = "client_id",
                ClientProtocol = "samlp",
                ClientAuthorizeQuery = "type=code\u0026timeout=30",
            },
        },
    });

});
```
```go
package main

import (
	"encoding/json"

	"github.com/pulumi/pulumi-auth0/sdk/v3/go/auth0"
	"github.com/pulumi/pulumi/sdk/v3/go/pulumi"
)

func main() {
	pulumi.Run(func(ctx *pulumi.Context) error {
		tmpJSON0, err := json.Marshal(map[string]interface{}{
			"name": []string{
				"name",
				"nameidentifier",
			},
			"email": []string{
				"emailaddress",
				"nameidentifier",
			},
			"family_name": "surname",
		})
		if err != nil {
			return err
		}
		json0 := string(tmpJSON0)
		// This is an example of a SAML connection.
		_, err = auth0.NewConnection(ctx, "samlp", \u0026auth0.ConnectionArgs{
			Name:     pulumi.String("SAML-Connection"),
			Strategy: pulumi.String("samlp"),
			Options: \u0026auth0.ConnectionOptionsArgs{
				Debug:           pulumi.Bool(false),
				SigningCert:     pulumi.String("\u003csigning-certificate\u003e"),
				SignInEndpoint:  pulumi.String("https://saml.provider/sign_in"),
				SignOutEndpoint: pulumi.String("https://saml.provider/sign_out"),
				DisableSignOut:  pulumi.Bool(true),
				TenantDomain:    pulumi.String("example.com"),
				DomainAliases: pulumi.StringArray{
					pulumi.String("example.com"),
					pulumi.String("alias.example.com"),
				},
				ProtocolBinding: pulumi.String("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"),
				RequestTemplate: pulumi.String(`\u003csamlp:AuthnRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
@@AssertServiceURLAndDestination@@
    ID="@@ID@@"
    IssueInstant="@@IssueInstant@@"
    ProtocolBinding="@@ProtocolBinding@@" Version="2.0"\u003e
    \u003csaml:Issuer xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"\u003e@@Issuer@@\u003c/saml:Issuer\u003e
\u003c/samlp:AuthnRequest\u003e`),
				UserIdAttribute:    pulumi.String("https://saml.provider/imi/ns/identity-200810"),
				SignatureAlgorithm: pulumi.String("rsa-sha256"),
				DigestAlgorithm:    pulumi.String("sha256"),
				IconUrl:            pulumi.String("https://saml.provider/assets/logo.png"),
				EntityId:           pulumi.String("\u003centity_id\u003e"),
				MetadataXml: pulumi.String(`    \u003c?xml version="1.0"?\u003e
    \u003cmd:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" xmlns:ds="http://www.w3.org/2000/09/xmldsig#" entityID="https://example.com"\u003e
      \u003cmd:IDPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol"\u003e
        \u003cmd:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://saml.provider/sign_out"/\u003e
        \u003cmd:SingleSignOnService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://saml.provider/sign_in"/\u003e
      \u003c/md:IDPSSODescriptor\u003e
    \u003c/md:EntityDescriptor\u003e
`),
				MetadataUrl: pulumi.String("https://saml.provider/imi/ns/FederationMetadata.xml"),
				FieldsMap:   pulumi.String(json0),
				SigningKey: \u0026auth0.ConnectionOptionsSigningKeyArgs{
					Key:  pulumi.String("-----BEGIN PRIVATE KEY-----\
...{your private key here}...\
-----END PRIVATE KEY-----"),
					Cert: pulumi.String("-----BEGIN CERTIFICATE-----\
...{your public key cert here}...\
-----END CERTIFICATE-----"),
				},
				DecryptionKey: \u0026auth0.ConnectionOptionsDecryptionKeyArgs{
					Key:  pulumi.String("-----BEGIN PRIVATE KEY-----\
...{your private key here}...\
-----END PRIVATE KEY-----"),
					Cert: pulumi.String("-----BEGIN CERTIFICATE-----\
...{your public key cert here}...\
-----END CERTIFICATE-----"),
				},
				IdpInitiated: \u0026auth0.ConnectionOptionsIdpInitiatedArgs{
					ClientId:             pulumi.String("client_id"),
					ClientProtocol:       pulumi.String("samlp"),
					ClientAuthorizeQuery: pulumi.String("type=code\u0026timeout=30"),
				},
			},
		})
		if err != nil {
			return err
		}
		return nil
	})
}
```
```java
package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.auth0.Connection;
import com.pulumi.auth0.ConnectionArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsSigningKeyArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsDecryptionKeyArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsIdpInitiatedArgs;
import static com.pulumi.codegen.internal.Serialization.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        // This is an example of a SAML connection.
        var samlp = new Connection("samlp", ConnectionArgs.builder()        
            .name("SAML-Connection")
            .strategy("samlp")
            .options(ConnectionOptionsArgs.builder()
                .debug(false)
                .signingCert("\u003csigning-certificate\u003e")
                .signInEndpoint("https://saml.provider/sign_in")
                .signOutEndpoint("https://saml.provider/sign_out")
                .disableSignOut(true)
                .tenantDomain("example.com")
                .domainAliases(                
                    "example.com",
                    "alias.example.com")
                .protocolBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST")
                .requestTemplate("""
\u003csamlp:AuthnRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
@@AssertServiceURLAndDestination@@
    ID="@@ID@@"
    IssueInstant="@@IssueInstant@@"
    ProtocolBinding="@@ProtocolBinding@@" Version="2.0"\u003e
    \u003csaml:Issuer xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"\u003e@@Issuer@@\u003c/saml:Issuer\u003e
\u003c/samlp:AuthnRequest\u003e                """)
                .userIdAttribute("https://saml.provider/imi/ns/identity-200810")
                .signatureAlgorithm("rsa-sha256")
                .digestAlgorithm("sha256")
                .iconUrl("https://saml.provider/assets/logo.png")
                .entityId("\u003centity_id\u003e")
                .metadataXml("""
    \u003c?xml version="1.0"?\u003e
    \u003cmd:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" xmlns:ds="http://www.w3.org/2000/09/xmldsig#" entityID="https://example.com"\u003e
      \u003cmd:IDPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol"\u003e
        \u003cmd:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://saml.provider/sign_out"/\u003e
        \u003cmd:SingleSignOnService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://saml.provider/sign_in"/\u003e
      \u003c/md:IDPSSODescriptor\u003e
    \u003c/md:EntityDescriptor\u003e
                """)
                .metadataUrl("https://saml.provider/imi/ns/FederationMetadata.xml")
                .fieldsMap(serializeJson(
                    jsonObject(
                        jsonProperty("name", jsonArray(
                            "name", 
                            "nameidentifier"
                        )),
                        jsonProperty("email", jsonArray(
                            "emailaddress", 
                            "nameidentifier"
                        )),
                        jsonProperty("family_name", "surname")
                    )))
                .signingKey(ConnectionOptionsSigningKeyArgs.builder()
                    .key("""
-----BEGIN PRIVATE KEY-----
...{your private key here}...
-----END PRIVATE KEY-----                    """)
                    .cert("""
-----BEGIN CERTIFICATE-----
...{your public key cert here}...
-----END CERTIFICATE-----                    """)
                    .build())
                .decryptionKey(ConnectionOptionsDecryptionKeyArgs.builder()
                    .key("""
-----BEGIN PRIVATE KEY-----
...{your private key here}...
-----END PRIVATE KEY-----                    """)
                    .cert("""
-----BEGIN CERTIFICATE-----
...{your public key cert here}...
-----END CERTIFICATE-----                    """)
                    .build())
                .idpInitiated(ConnectionOptionsIdpInitiatedArgs.builder()
                    .clientId("client_id")
                    .clientProtocol("samlp")
                    .clientAuthorizeQuery("type=code\u0026timeout=30")
                    .build())
                .build())
            .build());

    }
}
```
```yaml
resources:
  # This is an example of a SAML connection.
  samlp:
    type: auth0:Connection
    properties:
      name: SAML-Connection
      strategy: samlp
      options:
        debug: false
        signingCert: \u003csigning-certificate\u003e
        signInEndpoint: https://saml.provider/sign_in
        signOutEndpoint: https://saml.provider/sign_out
        disableSignOut: true
        tenantDomain: example.com
        domainAliases:
          - example.com
          - alias.example.com
        protocolBinding: urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST
        requestTemplate: |-
          \u003csamlp:AuthnRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
          @@AssertServiceURLAndDestination@@
              ID="@@ID@@"
              IssueInstant="@@IssueInstant@@"
              ProtocolBinding="@@ProtocolBinding@@" Version="2.0"\u003e
              \u003csaml:Issuer xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"\u003e@@Issuer@@\u003c/saml:Issuer\u003e
          \u003c/samlp:AuthnRequest\u003e
        userIdAttribute: https://saml.provider/imi/ns/identity-200810
        signatureAlgorithm: rsa-sha256
        digestAlgorithm: sha256
        iconUrl: https://saml.provider/assets/logo.png
        entityId: \u003centity_id\u003e
        metadataXml: |2
              \u003c?xml version="1.0"?\u003e
              \u003cmd:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" xmlns:ds="http://www.w3.org/2000/09/xmldsig#" entityID="https://example.com"\u003e
                \u003cmd:IDPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol"\u003e
                  \u003cmd:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://saml.provider/sign_out"/\u003e
                  \u003cmd:SingleSignOnService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://saml.provider/sign_in"/\u003e
                \u003c/md:IDPSSODescriptor\u003e
              \u003c/md:EntityDescriptor\u003e
        metadataUrl: https://saml.provider/imi/ns/FederationMetadata.xml
        fieldsMap:
          fn::toJSON:
            name:
              - name
              - nameidentifier
            email:
              - emailaddress
              - nameidentifier
            family_name: surname
        signingKey:
          key: |-
            -----BEGIN PRIVATE KEY-----
            ...{your private key here}...
            -----END PRIVATE KEY-----
          cert: |-
            -----BEGIN CERTIFICATE-----
            ...{your public key cert here}...
            -----END CERTIFICATE-----
        decryptionKey:
          key: |-
            -----BEGIN PRIVATE KEY-----
            ...{your private key here}...
            -----END PRIVATE KEY-----
          cert: |-
            -----BEGIN CERTIFICATE-----
            ...{your public key cert here}...
            -----END CERTIFICATE-----
        idpInitiated:
          clientId: client_id
          clientProtocol: samlp
          clientAuthorizeQuery: type=code\u0026timeout=30
```
\u003c!--End PulumiCodeChooser --\u003e

### WindowsLive Connection

\u003c!--Start PulumiCodeChooser --\u003e
```typescript
import * as pulumi from "@pulumi/pulumi";
import * as auth0 from "@pulumi/auth0";

// This is an example of a WindowsLive connection.
const windowslive = new auth0.Connection("windowslive", {
    name: "Windowslive-Connection",
    strategy: "windowslive",
    options: {
        clientId: "\u003cclient-id\u003e",
        clientSecret: "\u003cclient-secret\u003e",
        strategyVersion: 2,
        scopes: [
            "signin",
            "graph_user",
        ],
        setUserRootAttributes: "on_first_login",
        nonPersistentAttrs: [
            "ethnicity",
            "gender",
        ],
    },
});
```
```python
import pulumi
import pulumi_auth0 as auth0

# This is an example of a WindowsLive connection.
windowslive = auth0.Connection("windowslive",
    name="Windowslive-Connection",
    strategy="windowslive",
    options=auth0.ConnectionOptionsArgs(
        client_id="\u003cclient-id\u003e",
        client_secret="\u003cclient-secret\u003e",
        strategy_version=2,
        scopes=[
            "signin",
            "graph_user",
        ],
        set_user_root_attributes="on_first_login",
        non_persistent_attrs=[
            "ethnicity",
            "gender",
        ],
    ))
```
```csharp
using System.Collections.Generic;
using System.Linq;
using Pulumi;
using Auth0 = Pulumi.Auth0;

return await Deployment.RunAsync(() =\u003e 
{
    // This is an example of a WindowsLive connection.
    var windowslive = new Auth0.Connection("windowslive", new()
    {
        Name = "Windowslive-Connection",
        Strategy = "windowslive",
        Options = new Auth0.Inputs.ConnectionOptionsArgs
        {
            ClientId = "\u003cclient-id\u003e",
            ClientSecret = "\u003cclient-secret\u003e",
            StrategyVersion = 2,
            Scopes = new[]
            {
                "signin",
                "graph_user",
            },
            SetUserRootAttributes = "on_first_login",
            NonPersistentAttrs = new[]
            {
                "ethnicity",
                "gender",
            },
        },
    });

});
```
```go
package main

import (
	"github.com/pulumi/pulumi-auth0/sdk/v3/go/auth0"
	"github.com/pulumi/pulumi/sdk/v3/go/pulumi"
)

func main() {
	pulumi.Run(func(ctx *pulumi.Context) error {
		// This is an example of a WindowsLive connection.
		_, err := auth0.NewConnection(ctx, "windowslive", \u0026auth0.ConnectionArgs{
			Name:     pulumi.String("Windowslive-Connection"),
			Strategy: pulumi.String("windowslive"),
			Options: \u0026auth0.ConnectionOptionsArgs{
				ClientId:        pulumi.String("\u003cclient-id\u003e"),
				ClientSecret:    pulumi.String("\u003cclient-secret\u003e"),
				StrategyVersion: pulumi.Int(2),
				Scopes: pulumi.StringArray{
					pulumi.String("signin"),
					pulumi.String("graph_user"),
				},
				SetUserRootAttributes: pulumi.String("on_first_login"),
				NonPersistentAttrs: pulumi.StringArray{
					pulumi.String("ethnicity"),
					pulumi.String("gender"),
				},
			},
		})
		if err != nil {
			return err
		}
		return nil
	})
}
```
```java
package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.auth0.Connection;
import com.pulumi.auth0.ConnectionArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        // This is an example of a WindowsLive connection.
        var windowslive = new Connection("windowslive", ConnectionArgs.builder()        
            .name("Windowslive-Connection")
            .strategy("windowslive")
            .options(ConnectionOptionsArgs.builder()
                .clientId("\u003cclient-id\u003e")
                .clientSecret("\u003cclient-secret\u003e")
                .strategyVersion(2)
                .scopes(                
                    "signin",
                    "graph_user")
                .setUserRootAttributes("on_first_login")
                .nonPersistentAttrs(                
                    "ethnicity",
                    "gender")
                .build())
            .build());

    }
}
```
```yaml
resources:
  # This is an example of a WindowsLive connection.
  windowslive:
    type: auth0:Connection
    properties:
      name: Windowslive-Connection
      strategy: windowslive
      options:
        clientId: \u003cclient-id\u003e
        clientSecret: \u003cclient-secret\u003e
        strategyVersion: 2
        scopes:
          - signin
          - graph_user
        setUserRootAttributes: on_first_login
        nonPersistentAttrs:
          - ethnicity
          - gender
```
\u003c!--End PulumiCodeChooser --\u003e

### OIDC Connection

\u003c!--Start PulumiCodeChooser --\u003e
```typescript
import * as pulumi from "@pulumi/pulumi";
import * as auth0 from "@pulumi/auth0";

// This is an example of an OIDC connection.
const oidc = new auth0.Connection("oidc", {
    name: "oidc-connection",
    displayName: "OIDC Connection",
    strategy: "oidc",
    showAsButton: false,
    options: {
        clientId: "1234567",
        clientSecret: "1234567",
        domainAliases: ["example.com"],
        tenantDomain: "",
        iconUrl: "https://example.com/assets/logo.png",
        type: "back_channel",
        issuer: "https://www.paypalobjects.com",
        jwksUri: "https://api.paypal.com/v1/oauth2/certs",
        discoveryUrl: "https://www.paypalobjects.com/.well-known/openid-configuration",
        tokenEndpoint: "https://api.paypal.com/v1/oauth2/token",
        userinfoEndpoint: "https://api.paypal.com/v1/oauth2/token/userinfo",
        authorizationEndpoint: "https://www.paypal.com/signin/authorize",
        scopes: [
            "openid",
            "email",
        ],
        setUserRootAttributes: "on_first_login",
        nonPersistentAttrs: [
            "ethnicity",
            "gender",
        ],
        connectionSettings: {
            pkce: "auto",
        },
        attributeMap: {
            mappingMode: "use_map",
            userinfoScope: "openid email profile groups",
            attributes: JSON.stringify({
                name: "${context.tokenset.name}",
                email: "${context.tokenset.email}",
                email_verified: "${context.tokenset.email_verified}",
                nickname: "${context.tokenset.nickname}",
                picture: "${context.tokenset.picture}",
                given_name: "${context.tokenset.given_name}",
                family_name: "${context.tokenset.family_name}",
            }),
        },
    },
});
```
```python
import pulumi
import json
import pulumi_auth0 as auth0

# This is an example of an OIDC connection.
oidc = auth0.Connection("oidc",
    name="oidc-connection",
    display_name="OIDC Connection",
    strategy="oidc",
    show_as_button=False,
    options=auth0.ConnectionOptionsArgs(
        client_id="1234567",
        client_secret="1234567",
        domain_aliases=["example.com"],
        tenant_domain="",
        icon_url="https://example.com/assets/logo.png",
        type="back_channel",
        issuer="https://www.paypalobjects.com",
        jwks_uri="https://api.paypal.com/v1/oauth2/certs",
        discovery_url="https://www.paypalobjects.com/.well-known/openid-configuration",
        token_endpoint="https://api.paypal.com/v1/oauth2/token",
        userinfo_endpoint="https://api.paypal.com/v1/oauth2/token/userinfo",
        authorization_endpoint="https://www.paypal.com/signin/authorize",
        scopes=[
            "openid",
            "email",
        ],
        set_user_root_attributes="on_first_login",
        non_persistent_attrs=[
            "ethnicity",
            "gender",
        ],
        connection_settings=auth0.ConnectionOptionsConnectionSettingsArgs(
            pkce="auto",
        ),
        attribute_map=auth0.ConnectionOptionsAttributeMapArgs(
            mapping_mode="use_map",
            userinfo_scope="openid email profile groups",
            attributes=json.dumps({
                "name": "${context.tokenset.name}",
                "email": "${context.tokenset.email}",
                "email_verified": "${context.tokenset.email_verified}",
                "nickname": "${context.tokenset.nickname}",
                "picture": "${context.tokenset.picture}",
                "given_name": "${context.tokenset.given_name}",
                "family_name": "${context.tokenset.family_name}",
            }),
        ),
    ))
```
```csharp
using System.Collections.Generic;
using System.Linq;
using System.Text.Json;
using Pulumi;
using Auth0 = Pulumi.Auth0;

return await Deployment.RunAsync(() =\u003e 
{
    // This is an example of an OIDC connection.
    var oidc = new Auth0.Connection("oidc", new()
    {
        Name = "oidc-connection",
        DisplayName = "OIDC Connection",
        Strategy = "oidc",
        ShowAsButton = false,
        Options = new Auth0.Inputs.ConnectionOptionsArgs
        {
            ClientId = "1234567",
            ClientSecret = "1234567",
            DomainAliases = new[]
            {
                "example.com",
            },
            TenantDomain = "",
            IconUrl = "https://example.com/assets/logo.png",
            Type = "back_channel",
            Issuer = "https://www.paypalobjects.com",
            JwksUri = "https://api.paypal.com/v1/oauth2/certs",
            DiscoveryUrl = "https://www.paypalobjects.com/.well-known/openid-configuration",
            TokenEndpoint = "https://api.paypal.com/v1/oauth2/token",
            UserinfoEndpoint = "https://api.paypal.com/v1/oauth2/token/userinfo",
            AuthorizationEndpoint = "https://www.paypal.com/signin/authorize",
            Scopes = new[]
            {
                "openid",
                "email",
            },
            SetUserRootAttributes = "on_first_login",
            NonPersistentAttrs = new[]
            {
                "ethnicity",
                "gender",
            },
            ConnectionSettings = new Auth0.Inputs.ConnectionOptionsConnectionSettingsArgs
            {
                Pkce = "auto",
            },
            AttributeMap = new Auth0.Inputs.ConnectionOptionsAttributeMapArgs
            {
                MappingMode = "use_map",
                UserinfoScope = "openid email profile groups",
                Attributes = JsonSerializer.Serialize(new Dictionary\u003cstring, object?\u003e
                {
                    ["name"] = "${context.tokenset.name}",
                    ["email"] = "${context.tokenset.email}",
                    ["email_verified"] = "${context.tokenset.email_verified}",
                    ["nickname"] = "${context.tokenset.nickname}",
                    ["picture"] = "${context.tokenset.picture}",
                    ["given_name"] = "${context.tokenset.given_name}",
                    ["family_name"] = "${context.tokenset.family_name}",
                }),
            },
        },
    });

});
```
```go
package main

import (
	"encoding/json"

	"github.com/pulumi/pulumi-auth0/sdk/v3/go/auth0"
	"github.com/pulumi/pulumi/sdk/v3/go/pulumi"
)

func main() {
	pulumi.Run(func(ctx *pulumi.Context) error {
		tmpJSON0, err := json.Marshal(map[string]interface{}{
			"name":           "${context.tokenset.name}",
			"email":          "${context.tokenset.email}",
			"email_verified": "${context.tokenset.email_verified}",
			"nickname":       "${context.tokenset.nickname}",
			"picture":        "${context.tokenset.picture}",
			"given_name":     "${context.tokenset.given_name}",
			"family_name":    "${context.tokenset.family_name}",
		})
		if err != nil {
			return err
		}
		json0 := string(tmpJSON0)
		// This is an example of an OIDC connection.
		_, err = auth0.NewConnection(ctx, "oidc", \u0026auth0.ConnectionArgs{
			Name:         pulumi.String("oidc-connection"),
			DisplayName:  pulumi.String("OIDC Connection"),
			Strategy:     pulumi.String("oidc"),
			ShowAsButton: pulumi.Bool(false),
			Options: \u0026auth0.ConnectionOptionsArgs{
				ClientId:     pulumi.String("1234567"),
				ClientSecret: pulumi.String("1234567"),
				DomainAliases: pulumi.StringArray{
					pulumi.String("example.com"),
				},
				TenantDomain:          pulumi.String(""),
				IconUrl:               pulumi.String("https://example.com/assets/logo.png"),
				Type:                  pulumi.String("back_channel"),
				Issuer:                pulumi.String("https://www.paypalobjects.com"),
				JwksUri:               pulumi.String("https://api.paypal.com/v1/oauth2/certs"),
				DiscoveryUrl:          pulumi.String("https://www.paypalobjects.com/.well-known/openid-configuration"),
				TokenEndpoint:         pulumi.String("https://api.paypal.com/v1/oauth2/token"),
				UserinfoEndpoint:      pulumi.String("https://api.paypal.com/v1/oauth2/token/userinfo"),
				AuthorizationEndpoint: pulumi.String("https://www.paypal.com/signin/authorize"),
				Scopes: pulumi.StringArray{
					pulumi.String("openid"),
					pulumi.String("email"),
				},
				SetUserRootAttributes: pulumi.String("on_first_login"),
				NonPersistentAttrs: pulumi.StringArray{
					pulumi.String("ethnicity"),
					pulumi.String("gender"),
				},
				ConnectionSettings: \u0026auth0.ConnectionOptionsConnectionSettingsArgs{
					Pkce: pulumi.String("auto"),
				},
				AttributeMap: \u0026auth0.ConnectionOptionsAttributeMapArgs{
					MappingMode:   pulumi.String("use_map"),
					UserinfoScope: pulumi.String("openid email profile groups"),
					Attributes:    pulumi.String(json0),
				},
			},
		})
		if err != nil {
			return err
		}
		return nil
	})
}
```
```java
package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.auth0.Connection;
import com.pulumi.auth0.ConnectionArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsConnectionSettingsArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsAttributeMapArgs;
import static com.pulumi.codegen.internal.Serialization.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        // This is an example of an OIDC connection.
        var oidc = new Connection("oidc", ConnectionArgs.builder()        
            .name("oidc-connection")
            .displayName("OIDC Connection")
            .strategy("oidc")
            .showAsButton(false)
            .options(ConnectionOptionsArgs.builder()
                .clientId("1234567")
                .clientSecret("1234567")
                .domainAliases("example.com")
                .tenantDomain("")
                .iconUrl("https://example.com/assets/logo.png")
                .type("back_channel")
                .issuer("https://www.paypalobjects.com")
                .jwksUri("https://api.paypal.com/v1/oauth2/certs")
                .discoveryUrl("https://www.paypalobjects.com/.well-known/openid-configuration")
                .tokenEndpoint("https://api.paypal.com/v1/oauth2/token")
                .userinfoEndpoint("https://api.paypal.com/v1/oauth2/token/userinfo")
                .authorizationEndpoint("https://www.paypal.com/signin/authorize")
                .scopes(                
                    "openid",
                    "email")
                .setUserRootAttributes("on_first_login")
                .nonPersistentAttrs(                
                    "ethnicity",
                    "gender")
                .connectionSettings(ConnectionOptionsConnectionSettingsArgs.builder()
                    .pkce("auto")
                    .build())
                .attributeMap(ConnectionOptionsAttributeMapArgs.builder()
                    .mappingMode("use_map")
                    .userinfoScope("openid email profile groups")
                    .attributes(serializeJson(
                        jsonObject(
                            jsonProperty("name", "${context.tokenset.name}"),
                            jsonProperty("email", "${context.tokenset.email}"),
                            jsonProperty("email_verified", "${context.tokenset.email_verified}"),
                            jsonProperty("nickname", "${context.tokenset.nickname}"),
                            jsonProperty("picture", "${context.tokenset.picture}"),
                            jsonProperty("given_name", "${context.tokenset.given_name}"),
                            jsonProperty("family_name", "${context.tokenset.family_name}")
                        )))
                    .build())
                .build())
            .build());

    }
}
```
```yaml
resources:
  # This is an example of an OIDC connection.
  oidc:
    type: auth0:Connection
    properties:
      name: oidc-connection
      displayName: OIDC Connection
      strategy: oidc
      showAsButton: false
      options:
        clientId: '1234567'
        clientSecret: '1234567'
        domainAliases:
          - example.com
        tenantDomain:
        iconUrl: https://example.com/assets/logo.png
        type: back_channel
        issuer: https://www.paypalobjects.com
        jwksUri: https://api.paypal.com/v1/oauth2/certs
        discoveryUrl: https://www.paypalobjects.com/.well-known/openid-configuration
        tokenEndpoint: https://api.paypal.com/v1/oauth2/token
        userinfoEndpoint: https://api.paypal.com/v1/oauth2/token/userinfo
        authorizationEndpoint: https://www.paypal.com/signin/authorize
        scopes:
          - openid
          - email
        setUserRootAttributes: on_first_login
        nonPersistentAttrs:
          - ethnicity
          - gender
        connectionSettings:
          pkce: auto
        attributeMap:
          mappingMode: use_map
          userinfoScope: openid email profile groups
          attributes:
            fn::toJSON:
              name: ${context.tokenset.name}
              email: ${context.tokenset.email}
              email_verified: ${context.tokenset.email_verified}
              nickname: ${context.tokenset.nickname}
              picture: ${context.tokenset.picture}
              given_name: ${context.tokenset.given_name}
              family_name: ${context.tokenset.family_name}
```
\u003c!--End PulumiCodeChooser --\u003e

### Okta Connection

\u003c!--Start PulumiCodeChooser --\u003e
```typescript
import * as pulumi from "@pulumi/pulumi";
import * as auth0 from "@pulumi/auth0";

// This is an example of an Okta Workforce connection.
const okta = new auth0.Connection("okta", {
    name: "okta-connection",
    displayName: "Okta Workforce Connection",
    strategy: "okta",
    showAsButton: false,
    options: {
        clientId: "1234567",
        clientSecret: "1234567",
        domain: "example.okta.com",
        domainAliases: ["example.com"],
        issuer: "https://example.okta.com",
        jwksUri: "https://example.okta.com/oauth2/v1/keys",
        tokenEndpoint: "https://example.okta.com/oauth2/v1/token",
        userinfoEndpoint: "https://example.okta.com/oauth2/v1/userinfo",
        authorizationEndpoint: "https://example.okta.com/oauth2/v1/authorize",
        scopes: [
            "openid",
            "email",
        ],
        setUserRootAttributes: "on_first_login",
        nonPersistentAttrs: [
            "ethnicity",
            "gender",
        ],
        upstreamParams: JSON.stringify({
            screen_name: {
                alias: "login_hint",
            },
        }),
        connectionSettings: {
            pkce: "auto",
        },
        attributeMap: {
            mappingMode: "basic_profile",
            userinfoScope: "openid email profile groups",
            attributes: JSON.stringify({
                name: "${context.tokenset.name}",
                email: "${context.tokenset.email}",
                email_verified: "${context.tokenset.email_verified}",
                nickname: "${context.tokenset.nickname}",
                picture: "${context.tokenset.picture}",
                given_name: "${context.tokenset.given_name}",
                family_name: "${context.tokenset.family_name}",
            }),
        },
    },
});
```
```python
import pulumi
import json
import pulumi_auth0 as auth0

# This is an example of an Okta Workforce connection.
okta = auth0.Connection("okta",
    name="okta-connection",
    display_name="Okta Workforce Connection",
    strategy="okta",
    show_as_button=False,
    options=auth0.ConnectionOptionsArgs(
        client_id="1234567",
        client_secret="1234567",
        domain="example.okta.com",
        domain_aliases=["example.com"],
        issuer="https://example.okta.com",
        jwks_uri="https://example.okta.com/oauth2/v1/keys",
        token_endpoint="https://example.okta.com/oauth2/v1/token",
        userinfo_endpoint="https://example.okta.com/oauth2/v1/userinfo",
        authorization_endpoint="https://example.okta.com/oauth2/v1/authorize",
        scopes=[
            "openid",
            "email",
        ],
        set_user_root_attributes="on_first_login",
        non_persistent_attrs=[
            "ethnicity",
            "gender",
        ],
        upstream_params=json.dumps({
            "screen_name": {
                "alias": "login_hint",
            },
        }),
        connection_settings=auth0.ConnectionOptionsConnectionSettingsArgs(
            pkce="auto",
        ),
        attribute_map=auth0.ConnectionOptionsAttributeMapArgs(
            mapping_mode="basic_profile",
            userinfo_scope="openid email profile groups",
            attributes=json.dumps({
                "name": "${context.tokenset.name}",
                "email": "${context.tokenset.email}",
                "email_verified": "${context.tokenset.email_verified}",
                "nickname": "${context.tokenset.nickname}",
                "picture": "${context.tokenset.picture}",
                "given_name": "${context.tokenset.given_name}",
                "family_name": "${context.tokenset.family_name}",
            }),
        ),
    ))
```
```csharp
using System.Collections.Generic;
using System.Linq;
using System.Text.Json;
using Pulumi;
using Auth0 = Pulumi.Auth0;

return await Deployment.RunAsync(() =\u003e 
{
    // This is an example of an Okta Workforce connection.
    var okta = new Auth0.Connection("okta", new()
    {
        Name = "okta-connection",
        DisplayName = "Okta Workforce Connection",
        Strategy = "okta",
        ShowAsButton = false,
        Options = new Auth0.Inputs.ConnectionOptionsArgs
        {
            ClientId = "1234567",
            ClientSecret = "1234567",
            Domain = "example.okta.com",
            DomainAliases = new[]
            {
                "example.com",
            },
            Issuer = "https://example.okta.com",
            JwksUri = "https://example.okta.com/oauth2/v1/keys",
            TokenEndpoint = "https://example.okta.com/oauth2/v1/token",
            UserinfoEndpoint = "https://example.okta.com/oauth2/v1/userinfo",
            AuthorizationEndpoint = "https://example.okta.com/oauth2/v1/authorize",
            Scopes = new[]
            {
                "openid",
                "email",
            },
            SetUserRootAttributes = "on_first_login",
            NonPersistentAttrs = new[]
            {
                "ethnicity",
                "gender",
            },
            UpstreamParams = JsonSerializer.Serialize(new Dictionary\u003cstring, object?\u003e
            {
                ["screen_name"] = new Dictionary\u003cstring, object?\u003e
                {
                    ["alias"] = "login_hint",
                },
            }),
            ConnectionSettings = new Auth0.Inputs.ConnectionOptionsConnectionSettingsArgs
            {
                Pkce = "auto",
            },
            AttributeMap = new Auth0.Inputs.ConnectionOptionsAttributeMapArgs
            {
                MappingMode = "basic_profile",
                UserinfoScope = "openid email profile groups",
                Attributes = JsonSerializer.Serialize(new Dictionary\u003cstring, object?\u003e
                {
                    ["name"] = "${context.tokenset.name}",
                    ["email"] = "${context.tokenset.email}",
                    ["email_verified"] = "${context.tokenset.email_verified}",
                    ["nickname"] = "${context.tokenset.nickname}",
                    ["picture"] = "${context.tokenset.picture}",
                    ["given_name"] = "${context.tokenset.given_name}",
                    ["family_name"] = "${context.tokenset.family_name}",
                }),
            },
        },
    });

});
```
```go
package main

import (
	"encoding/json"

	"github.com/pulumi/pulumi-auth0/sdk/v3/go/auth0"
	"github.com/pulumi/pulumi/sdk/v3/go/pulumi"
)

func main() {
	pulumi.Run(func(ctx *pulumi.Context) error {
		tmpJSON0, err := json.Marshal(map[string]interface{}{
			"screen_name": map[string]interface{}{
				"alias": "login_hint",
			},
		})
		if err != nil {
			return err
		}
		json0 := string(tmpJSON0)
		tmpJSON1, err := json.Marshal(map[string]interface{}{
			"name":           "${context.tokenset.name}",
			"email":          "${context.tokenset.email}",
			"email_verified": "${context.tokenset.email_verified}",
			"nickname":       "${context.tokenset.nickname}",
			"picture":        "${context.tokenset.picture}",
			"given_name":     "${context.tokenset.given_name}",
			"family_name":    "${context.tokenset.family_name}",
		})
		if err != nil {
			return err
		}
		json1 := string(tmpJSON1)
		// This is an example of an Okta Workforce connection.
		_, err = auth0.NewConnection(ctx, "okta", \u0026auth0.ConnectionArgs{
			Name:         pulumi.String("okta-connection"),
			DisplayName:  pulumi.String("Okta Workforce Connection"),
			Strategy:     pulumi.String("okta"),
			ShowAsButton: pulumi.Bool(false),
			Options: \u0026auth0.ConnectionOptionsArgs{
				ClientId:     pulumi.String("1234567"),
				ClientSecret: pulumi.String("1234567"),
				Domain:       pulumi.String("example.okta.com"),
				DomainAliases: pulumi.StringArray{
					pulumi.String("example.com"),
				},
				Issuer:                pulumi.String("https://example.okta.com"),
				JwksUri:               pulumi.String("https://example.okta.com/oauth2/v1/keys"),
				TokenEndpoint:         pulumi.String("https://example.okta.com/oauth2/v1/token"),
				UserinfoEndpoint:      pulumi.String("https://example.okta.com/oauth2/v1/userinfo"),
				AuthorizationEndpoint: pulumi.String("https://example.okta.com/oauth2/v1/authorize"),
				Scopes: pulumi.StringArray{
					pulumi.String("openid"),
					pulumi.String("email"),
				},
				SetUserRootAttributes: pulumi.String("on_first_login"),
				NonPersistentAttrs: pulumi.StringArray{
					pulumi.String("ethnicity"),
					pulumi.String("gender"),
				},
				UpstreamParams: pulumi.String(json0),
				ConnectionSettings: \u0026auth0.ConnectionOptionsConnectionSettingsArgs{
					Pkce: pulumi.String("auto"),
				},
				AttributeMap: \u0026auth0.ConnectionOptionsAttributeMapArgs{
					MappingMode:   pulumi.String("basic_profile"),
					UserinfoScope: pulumi.String("openid email profile groups"),
					Attributes:    pulumi.String(json1),
				},
			},
		})
		if err != nil {
			return err
		}
		return nil
	})
}
```
```java
package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.auth0.Connection;
import com.pulumi.auth0.ConnectionArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsConnectionSettingsArgs;
import com.pulumi.auth0.inputs.ConnectionOptionsAttributeMapArgs;
import static com.pulumi.codegen.internal.Serialization.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        // This is an example of an Okta Workforce connection.
        var okta = new Connection("okta", ConnectionArgs.builder()        
            .name("okta-connection")
            .displayName("Okta Workforce Connection")
            .strategy("okta")
            .showAsButton(false)
            .options(ConnectionOptionsArgs.builder()
                .clientId("1234567")
                .clientSecret("1234567")
                .domain("example.okta.com")
                .domainAliases("example.com")
                .issuer("https://example.okta.com")
                .jwksUri("https://example.okta.com/oauth2/v1/keys")
                .tokenEndpoint("https://example.okta.com/oauth2/v1/token")
                .userinfoEndpoint("https://example.okta.com/oauth2/v1/userinfo")
                .authorizationEndpoint("https://example.okta.com/oauth2/v1/authorize")
                .scopes(                
                    "openid",
                    "email")
                .setUserRootAttributes("on_first_login")
                .nonPersistentAttrs(                
                    "ethnicity",
                    "gender")
                .upstreamParams(serializeJson(
                    jsonObject(
                        jsonProperty("screen_name", jsonObject(
                            jsonProperty("alias", "login_hint")
                        ))
                    )))
                .connectionSettings(ConnectionOptionsConnectionSettingsArgs.builder()
                    .pkce("auto")
                    .build())
                .attributeMap(ConnectionOptionsAttributeMapArgs.builder()
                    .mappingMode("basic_profile")
                    .userinfoScope("openid email profile groups")
                    .attributes(serializeJson(
                        jsonObject(
                            jsonProperty("name", "${context.tokenset.name}"),
                            jsonProperty("email", "${context.tokenset.email}"),
                            jsonProperty("email_verified", "${context.tokenset.email_verified}"),
                            jsonProperty("nickname", "${context.tokenset.nickname}"),
                            jsonProperty("picture", "${context.tokenset.picture}"),
                            jsonProperty("given_name", "${context.tokenset.given_name}"),
                            jsonProperty("family_name", "${context.tokenset.family_name}")
                        )))
                    .build())
                .build())
            .build());

    }
}
```
```yaml
resources:
  # This is an example of an Okta Workforce connection.
  okta:
    type: auth0:Connection
    properties:
      name: okta-connection
      displayName: Okta Workforce Connection
      strategy: okta
      showAsButton: false
      options:
        clientId: '1234567'
        clientSecret: '1234567'
        domain: example.okta.com
        domainAliases:
          - example.com
        issuer: https://example.okta.com
        jwksUri: https://example.okta.com/oauth2/v1/keys
        tokenEndpoint: https://example.okta.com/oauth2/v1/token
        userinfoEndpoint: https://example.okta.com/oauth2/v1/userinfo
        authorizationEndpoint: https://example.okta.com/oauth2/v1/authorize
        scopes:
          - openid
          - email
        setUserRootAttributes: on_first_login
        nonPersistentAttrs:
          - ethnicity
          - gender
        upstreamParams:
          fn::toJSON:
            screen_name:
              alias: login_hint
        connectionSettings:
          pkce: auto
        attributeMap:
          mappingMode: basic_profile
          userinfoScope: openid email profile groups
          attributes:
            fn::toJSON:
              name: ${context.tokenset.name}
              email: ${context.tokenset.email}
              email_verified: ${context.tokenset.email_verified}
              nickname: ${context.tokenset.nickname}
              picture: ${context.tokenset.picture}
              given_name: ${context.tokenset.given_name}
              family_name: ${context.tokenset.family_name}
```
\u003c!--End PulumiCodeChooser --\u003e

## Import

This resource can be imported by specifying the connection ID.

#

Example:

```sh
$ pulumi import auth0:index/connection:Connection google "con_a17f21fdb24d48a0"
```

