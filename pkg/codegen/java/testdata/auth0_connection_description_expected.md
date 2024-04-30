 * With Auth0, you can define sources of users, otherwise known as connections, which may include identity providers (such as Google or LinkedIn), databases, or passwordless authentication methods. This resource allows you to configure and manage connections to be used with your clients and users.
 * 
 * \u003e The Auth0 dashboard displays only one connection per social provider. Although the Auth0 Management API allows the
 * creation of multiple connections per strategy, the additional connections may not be visible in the Auth0 dashboard.
 * 
 * ## Example Usage
 * 
 * ### Auth0 Connection
 * 
 * \u003c!--Start PulumiCodeChooser --\u003e
 * {@code
 * package generated_program;
 * 
 * import com.pulumi.Context;
 * import com.pulumi.Pulumi;
 * import com.pulumi.core.Output;
 * import com.pulumi.auth0.Connection;
 * import com.pulumi.auth0.ConnectionArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsPasswordNoPersonalInfoArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsPasswordDictionaryArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsPasswordComplexityOptionsArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsValidationArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsValidationUsernameArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsMfaArgs;
 * import static com.pulumi.codegen.internal.Serialization.*;
 * import java.util.List;
 * import java.util.ArrayList;
 * import java.util.Map;
 * import java.io.File;
 * import java.nio.file.Files;
 * import java.nio.file.Paths;
 * 
 * public class App {
 *     public static void main(String[] args) {
 *         Pulumi.run(App::stack);
 *     }
 * 
 *     public static void stack(Context ctx) {
 *         // This is an example of an Auth0 connection.
 *         var myConnection = new Connection("myConnection", ConnectionArgs.builder()        
 *             .name("Example-Connection")
 *             .isDomainConnection(true)
 *             .strategy("auth0")
 *             .metadata(Map.ofEntries(
 *                 Map.entry("key1", "foo"),
 *                 Map.entry("key2", "bar")
 *             ))
 *             .options(ConnectionOptionsArgs.builder()
 *                 .passwordPolicy("excellent")
 *                 .bruteForceProtection(true)
 *                 .enabledDatabaseCustomization(true)
 *                 .importMode(false)
 *                 .requiresUsername(true)
 *                 .disableSignup(false)
 *                 .customScripts(Map.of("get_user", """
 *         function getByEmail(email, callback) {
 *           return callback(new Error("Whoops!"));
 *         }
 *                 """))
 *                 .configuration(Map.ofEntries(
 *                     Map.entry("foo", "bar"),
 *                     Map.entry("bar", "baz")
 *                 ))
 *                 .upstreamParams(serializeJson(
 *                     jsonObject(
 *                         jsonProperty("screen_name", jsonObject(
 *                             jsonProperty("alias", "login_hint")
 *                         ))
 *                     )))
 *                 .passwordHistories(ConnectionOptionsPasswordHistoryArgs.builder()
 *                     .enable(true)
 *                     .size(3)
 *                     .build())
 *                 .passwordNoPersonalInfo(ConnectionOptionsPasswordNoPersonalInfoArgs.builder()
 *                     .enable(true)
 *                     .build())
 *                 .passwordDictionary(ConnectionOptionsPasswordDictionaryArgs.builder()
 *                     .enable(true)
 *                     .dictionaries(                    
 *                         "password",
 *                         "admin",
 *                         "1234")
 *                     .build())
 *                 .passwordComplexityOptions(ConnectionOptionsPasswordComplexityOptionsArgs.builder()
 *                     .minLength(12)
 *                     .build())
 *                 .validation(ConnectionOptionsValidationArgs.builder()
 *                     .username(ConnectionOptionsValidationUsernameArgs.builder()
 *                         .min(10)
 *                         .max(40)
 *                         .build())
 *                     .build())
 *                 .mfa(ConnectionOptionsMfaArgs.builder()
 *                     .active(true)
 *                     .returnEnrollSettings(true)
 *                     .build())
 *                 .build())
 *             .build());
 * 
 *     }
 * }
 * }
 * \u003c!--End PulumiCodeChooser --\u003e
 * 
 * ### Google OAuth2 Connection
 * 
 * \u003e Your Auth0 account may be pre-configured with a `google-oauth2` connection.
 * 
 * \u003c!--Start PulumiCodeChooser --\u003e
 * {@code
 * package generated_program;
 * 
 * import com.pulumi.Context;
 * import com.pulumi.Pulumi;
 * import com.pulumi.core.Output;
 * import com.pulumi.auth0.Connection;
 * import com.pulumi.auth0.ConnectionArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
 * import java.util.List;
 * import java.util.ArrayList;
 * import java.util.Map;
 * import java.io.File;
 * import java.nio.file.Files;
 * import java.nio.file.Paths;
 * 
 * public class App {
 *     public static void main(String[] args) {
 *         Pulumi.run(App::stack);
 *     }
 * 
 *     public static void stack(Context ctx) {
 *         // This is an example of a Google OAuth2 connection.
 *         var googleOauth2 = new Connection("googleOauth2", ConnectionArgs.builder()        
 *             .name("Google-OAuth2-Connection")
 *             .strategy("google-oauth2")
 *             .options(ConnectionOptionsArgs.builder()
 *                 .clientId("\u003cclient-id\u003e")
 *                 .clientSecret("\u003cclient-secret\u003e")
 *                 .allowedAudiences(                
 *                     "example.com",
 *                     "api.example.com")
 *                 .scopes(                
 *                     "email",
 *                     "profile",
 *                     "gmail",
 *                     "youtube")
 *                 .setUserRootAttributes("on_each_login")
 *                 .nonPersistentAttrs(                
 *                     "ethnicity",
 *                     "gender")
 *                 .build())
 *             .build());
 * 
 *     }
 * }
 * }
 * \u003c!--End PulumiCodeChooser --\u003e
 * 
 * ### Google Apps
 * 
 * \u003c!--Start PulumiCodeChooser --\u003e
 * {@code
 * package generated_program;
 * 
 * import com.pulumi.Context;
 * import com.pulumi.Pulumi;
 * import com.pulumi.core.Output;
 * import com.pulumi.auth0.Connection;
 * import com.pulumi.auth0.ConnectionArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
 * import static com.pulumi.codegen.internal.Serialization.*;
 * import java.util.List;
 * import java.util.ArrayList;
 * import java.util.Map;
 * import java.io.File;
 * import java.nio.file.Files;
 * import java.nio.file.Paths;
 * 
 * public class App {
 *     public static void main(String[] args) {
 *         Pulumi.run(App::stack);
 *     }
 * 
 *     public static void stack(Context ctx) {
 *         var googleApps = new Connection("googleApps", ConnectionArgs.builder()        
 *             .name("connection-google-apps")
 *             .isDomainConnection(false)
 *             .strategy("google-apps")
 *             .showAsButton(false)
 *             .options(ConnectionOptionsArgs.builder()
 *                 .clientId("")
 *                 .clientSecret("")
 *                 .domain("example.com")
 *                 .tenantDomain("example.com")
 *                 .domainAliases(                
 *                     "example.com",
 *                     "api.example.com")
 *                 .apiEnableUsers(true)
 *                 .scopes(                
 *                     "ext_profile",
 *                     "ext_groups")
 *                 .iconUrl("https://example.com/assets/logo.png")
 *                 .upstreamParams(serializeJson(
 *                     jsonObject(
 *                         jsonProperty("screen_name", jsonObject(
 *                             jsonProperty("alias", "login_hint")
 *                         ))
 *                     )))
 *                 .setUserRootAttributes("on_each_login")
 *                 .nonPersistentAttrs(                
 *                     "ethnicity",
 *                     "gender")
 *                 .build())
 *             .build());
 * 
 *     }
 * }
 * }
 * \u003c!--End PulumiCodeChooser --\u003e
 * 
 * ### Facebook Connection
 * 
 * \u003c!--Start PulumiCodeChooser --\u003e
 * {@code
 * package generated_program;
 * 
 * import com.pulumi.Context;
 * import com.pulumi.Pulumi;
 * import com.pulumi.core.Output;
 * import com.pulumi.auth0.Connection;
 * import com.pulumi.auth0.ConnectionArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
 * import java.util.List;
 * import java.util.ArrayList;
 * import java.util.Map;
 * import java.io.File;
 * import java.nio.file.Files;
 * import java.nio.file.Paths;
 * 
 * public class App {
 *     public static void main(String[] args) {
 *         Pulumi.run(App::stack);
 *     }
 * 
 *     public static void stack(Context ctx) {
 *         // This is an example of a Facebook connection.
 *         var facebook = new Connection("facebook", ConnectionArgs.builder()        
 *             .name("Facebook-Connection")
 *             .strategy("facebook")
 *             .options(ConnectionOptionsArgs.builder()
 *                 .clientId("\u003cclient-id\u003e")
 *                 .clientSecret("\u003cclient-secret\u003e")
 *                 .scopes(                
 *                     "public_profile",
 *                     "email",
 *                     "groups_access_member_info",
 *                     "user_birthday")
 *                 .setUserRootAttributes("on_each_login")
 *                 .nonPersistentAttrs(                
 *                     "ethnicity",
 *                     "gender")
 *                 .build())
 *             .build());
 * 
 *     }
 * }
 * }
 * \u003c!--End PulumiCodeChooser --\u003e
 * 
 * ### Apple Connection
 * 
 * \u003c!--Start PulumiCodeChooser --\u003e
 * {@code
 * package generated_program;
 * 
 * import com.pulumi.Context;
 * import com.pulumi.Pulumi;
 * import com.pulumi.core.Output;
 * import com.pulumi.auth0.Connection;
 * import com.pulumi.auth0.ConnectionArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
 * import java.util.List;
 * import java.util.ArrayList;
 * import java.util.Map;
 * import java.io.File;
 * import java.nio.file.Files;
 * import java.nio.file.Paths;
 * 
 * public class App {
 *     public static void main(String[] args) {
 *         Pulumi.run(App::stack);
 *     }
 * 
 *     public static void stack(Context ctx) {
 *         // This is an example of an Apple connection.
 *         var apple = new Connection("apple", ConnectionArgs.builder()        
 *             .name("Apple-Connection")
 *             .strategy("apple")
 *             .options(ConnectionOptionsArgs.builder()
 *                 .clientId("\u003cclient-id\u003e")
 *                 .clientSecret("""
 * -----BEGIN PRIVATE KEY-----
 * MIHBAgEAMA0GCSqGSIb3DQEBAQUABIGsMIGpAgEAA
 * -----END PRIVATE KEY-----                """)
 *                 .teamId("\u003cteam-id\u003e")
 *                 .keyId("\u003ckey-id\u003e")
 *                 .scopes(                
 *                     "email",
 *                     "name")
 *                 .setUserRootAttributes("on_first_login")
 *                 .nonPersistentAttrs(                
 *                     "ethnicity",
 *                     "gender")
 *                 .build())
 *             .build());
 * 
 *     }
 * }
 * }
 * \u003c!--End PulumiCodeChooser --\u003e
 * 
 * ### LinkedIn Connection
 * 
 * \u003c!--Start PulumiCodeChooser --\u003e
 * {@code
 * package generated_program;
 * 
 * import com.pulumi.Context;
 * import com.pulumi.Pulumi;
 * import com.pulumi.core.Output;
 * import com.pulumi.auth0.Connection;
 * import com.pulumi.auth0.ConnectionArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
 * import java.util.List;
 * import java.util.ArrayList;
 * import java.util.Map;
 * import java.io.File;
 * import java.nio.file.Files;
 * import java.nio.file.Paths;
 * 
 * public class App {
 *     public static void main(String[] args) {
 *         Pulumi.run(App::stack);
 *     }
 * 
 *     public static void stack(Context ctx) {
 *         // This is an example of an LinkedIn connection.
 *         var linkedin = new Connection("linkedin", ConnectionArgs.builder()        
 *             .name("Linkedin-Connection")
 *             .strategy("linkedin")
 *             .options(ConnectionOptionsArgs.builder()
 *                 .clientId("\u003cclient-id\u003e")
 *                 .clientSecret("\u003cclient-secret\u003e")
 *                 .strategyVersion(2)
 *                 .scopes(                
 *                     "basic_profile",
 *                     "profile",
 *                     "email")
 *                 .setUserRootAttributes("on_each_login")
 *                 .nonPersistentAttrs(                
 *                     "ethnicity",
 *                     "gender")
 *                 .build())
 *             .build());
 * 
 *     }
 * }
 * }
 * \u003c!--End PulumiCodeChooser --\u003e
 * 
 * ### GitHub Connection
 * 
 * \u003c!--Start PulumiCodeChooser --\u003e
 * {@code
 * package generated_program;
 * 
 * import com.pulumi.Context;
 * import com.pulumi.Pulumi;
 * import com.pulumi.core.Output;
 * import com.pulumi.auth0.Connection;
 * import com.pulumi.auth0.ConnectionArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
 * import java.util.List;
 * import java.util.ArrayList;
 * import java.util.Map;
 * import java.io.File;
 * import java.nio.file.Files;
 * import java.nio.file.Paths;
 * 
 * public class App {
 *     public static void main(String[] args) {
 *         Pulumi.run(App::stack);
 *     }
 * 
 *     public static void stack(Context ctx) {
 *         // This is an example of an GitHub connection.
 *         var github = new Connection("github", ConnectionArgs.builder()        
 *             .name("GitHub-Connection")
 *             .strategy("github")
 *             .options(ConnectionOptionsArgs.builder()
 *                 .clientId("\u003cclient-id\u003e")
 *                 .clientSecret("\u003cclient-secret\u003e")
 *                 .scopes(                
 *                     "email",
 *                     "profile",
 *                     "public_repo",
 *                     "repo")
 *                 .setUserRootAttributes("on_each_login")
 *                 .nonPersistentAttrs(                
 *                     "ethnicity",
 *                     "gender")
 *                 .build())
 *             .build());
 * 
 *     }
 * }
 * }
 * \u003c!--End PulumiCodeChooser --\u003e
 * 
 * ### SalesForce Connection
 * 
 * \u003c!--Start PulumiCodeChooser --\u003e
 * {@code
 * package generated_program;
 * 
 * import com.pulumi.Context;
 * import com.pulumi.Pulumi;
 * import com.pulumi.core.Output;
 * import com.pulumi.auth0.Connection;
 * import com.pulumi.auth0.ConnectionArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
 * import java.util.List;
 * import java.util.ArrayList;
 * import java.util.Map;
 * import java.io.File;
 * import java.nio.file.Files;
 * import java.nio.file.Paths;
 * 
 * public class App {
 *     public static void main(String[] args) {
 *         Pulumi.run(App::stack);
 *     }
 * 
 *     public static void stack(Context ctx) {
 *         // This is an example of an SalesForce connection.
 *         var salesforce = new Connection("salesforce", ConnectionArgs.builder()        
 *             .name("Salesforce-Connection")
 *             .strategy("salesforce")
 *             .options(ConnectionOptionsArgs.builder()
 *                 .clientId("\u003cclient-id\u003e")
 *                 .clientSecret("\u003cclient-secret\u003e")
 *                 .communityBaseUrl("https://salesforce.example.com")
 *                 .scopes(                
 *                     "openid",
 *                     "email")
 *                 .setUserRootAttributes("on_first_login")
 *                 .nonPersistentAttrs(                
 *                     "ethnicity",
 *                     "gender")
 *                 .build())
 *             .build());
 * 
 *     }
 * }
 * }
 * \u003c!--End PulumiCodeChooser --\u003e
 * 
 * ### OAuth2 Connection
 * 
 * Also applies to following connection strategies: `dropbox`, `bitbucket`, `paypal`, `twitter`, `amazon`, `yahoo`, `box`, `wordpress`, `shopify`, `custom`
 * 
 * \u003c!--Start PulumiCodeChooser --\u003e
 * {@code
 * package generated_program;
 * 
 * import com.pulumi.Context;
 * import com.pulumi.Pulumi;
 * import com.pulumi.core.Output;
 * import com.pulumi.auth0.Connection;
 * import com.pulumi.auth0.ConnectionArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
 * import java.util.List;
 * import java.util.ArrayList;
 * import java.util.Map;
 * import java.io.File;
 * import java.nio.file.Files;
 * import java.nio.file.Paths;
 * 
 * public class App {
 *     public static void main(String[] args) {
 *         Pulumi.run(App::stack);
 *     }
 * 
 *     public static void stack(Context ctx) {
 *         // This is an example of an OAuth2 connection.
 *         var oauth2 = new Connection("oauth2", ConnectionArgs.builder()        
 *             .name("OAuth2-Connection")
 *             .strategy("oauth2")
 *             .options(ConnectionOptionsArgs.builder()
 *                 .clientId("\u003cclient-id\u003e")
 *                 .clientSecret("\u003cclient-secret\u003e")
 *                 .scopes(                
 *                     "basic_profile",
 *                     "profile",
 *                     "email")
 *                 .tokenEndpoint("https://auth.example.com/oauth2/token")
 *                 .authorizationEndpoint("https://auth.example.com/oauth2/authorize")
 *                 .pkceEnabled(true)
 *                 .iconUrl("https://auth.example.com/assets/logo.png")
 *                 .scripts(Map.of("fetchUserProfile", """
 *         function fetchUserProfile(accessToken, context, callback) {
 *           return callback(new Error("Whoops!"));
 *         }
 *                 """))
 *                 .setUserRootAttributes("on_each_login")
 *                 .nonPersistentAttrs(                
 *                     "ethnicity",
 *                     "gender")
 *                 .build())
 *             .build());
 * 
 *     }
 * }
 * }
 * \u003c!--End PulumiCodeChooser --\u003e
 * 
 * ### Active Directory (AD)
 * 
 * \u003c!--Start PulumiCodeChooser --\u003e
 * {@code
 * package generated_program;
 * 
 * import com.pulumi.Context;
 * import com.pulumi.Pulumi;
 * import com.pulumi.core.Output;
 * import com.pulumi.auth0.Connection;
 * import com.pulumi.auth0.ConnectionArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
 * import static com.pulumi.codegen.internal.Serialization.*;
 * import java.util.List;
 * import java.util.ArrayList;
 * import java.util.Map;
 * import java.io.File;
 * import java.nio.file.Files;
 * import java.nio.file.Paths;
 * 
 * public class App {
 *     public static void main(String[] args) {
 *         Pulumi.run(App::stack);
 *     }
 * 
 *     public static void stack(Context ctx) {
 *         var ad = new Connection("ad", ConnectionArgs.builder()        
 *             .name("connection-active-directory")
 *             .displayName("Active Directory Connection")
 *             .strategy("ad")
 *             .showAsButton(true)
 *             .options(ConnectionOptionsArgs.builder()
 *                 .disableSelfServiceChangePassword(true)
 *                 .bruteForceProtection(true)
 *                 .tenantDomain("example.com")
 *                 .iconUrl("https://example.com/assets/logo.png")
 *                 .domainAliases(                
 *                     "example.com",
 *                     "api.example.com")
 *                 .ips(                
 *                     "192.168.1.1",
 *                     "192.168.1.2")
 *                 .setUserRootAttributes("on_each_login")
 *                 .nonPersistentAttrs(                
 *                     "ethnicity",
 *                     "gender")
 *                 .upstreamParams(serializeJson(
 *                     jsonObject(
 *                         jsonProperty("screen_name", jsonObject(
 *                             jsonProperty("alias", "login_hint")
 *                         ))
 *                     )))
 *                 .useCertAuth(false)
 *                 .useKerberos(false)
 *                 .disableCache(false)
 *                 .build())
 *             .build());
 * 
 *     }
 * }
 * }
 * \u003c!--End PulumiCodeChooser --\u003e
 * 
 * ### Azure AD Connection
 * 
 * \u003c!--Start PulumiCodeChooser --\u003e
 * {@code
 * package generated_program;
 * 
 * import com.pulumi.Context;
 * import com.pulumi.Pulumi;
 * import com.pulumi.core.Output;
 * import com.pulumi.auth0.Connection;
 * import com.pulumi.auth0.ConnectionArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
 * import static com.pulumi.codegen.internal.Serialization.*;
 * import java.util.List;
 * import java.util.ArrayList;
 * import java.util.Map;
 * import java.io.File;
 * import java.nio.file.Files;
 * import java.nio.file.Paths;
 * 
 * public class App {
 *     public static void main(String[] args) {
 *         Pulumi.run(App::stack);
 *     }
 * 
 *     public static void stack(Context ctx) {
 *         var azureAd = new Connection("azureAd", ConnectionArgs.builder()        
 *             .name("connection-azure-ad")
 *             .strategy("waad")
 *             .showAsButton(true)
 *             .options(ConnectionOptionsArgs.builder()
 *                 .identityApi("azure-active-directory-v1.0")
 *                 .clientId("123456")
 *                 .clientSecret("123456")
 *                 .appId("app-id-123")
 *                 .tenantDomain("example.onmicrosoft.com")
 *                 .domain("example.onmicrosoft.com")
 *                 .domainAliases(                
 *                     "example.com",
 *                     "api.example.com")
 *                 .iconUrl("https://example.onmicrosoft.com/assets/logo.png")
 *                 .useWsfed(false)
 *                 .waadProtocol("openid-connect")
 *                 .waadCommonEndpoint(false)
 *                 .maxGroupsToRetrieve(250)
 *                 .apiEnableUsers(true)
 *                 .scopes(                
 *                     "basic_profile",
 *                     "ext_groups",
 *                     "ext_profile")
 *                 .setUserRootAttributes("on_each_login")
 *                 .shouldTrustEmailVerifiedConnection("never_set_emails_as_verified")
 *                 .upstreamParams(serializeJson(
 *                     jsonObject(
 *                         jsonProperty("screen_name", jsonObject(
 *                             jsonProperty("alias", "login_hint")
 *                         ))
 *                     )))
 *                 .nonPersistentAttrs(                
 *                     "ethnicity",
 *                     "gender")
 *                 .build())
 *             .build());
 * 
 *     }
 * }
 * }
 * \u003c!--End PulumiCodeChooser --\u003e
 * 
 * ### Email Connection
 * 
 * \u003e To be able to see this in the management dashboard as well, the name of the connection must be set to &#34;email&#34;.
 * 
 * \u003c!--Start PulumiCodeChooser --\u003e
 * {@code
 * package generated_program;
 * 
 * import com.pulumi.Context;
 * import com.pulumi.Pulumi;
 * import com.pulumi.core.Output;
 * import com.pulumi.auth0.Connection;
 * import com.pulumi.auth0.ConnectionArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsTotpArgs;
 * import java.util.List;
 * import java.util.ArrayList;
 * import java.util.Map;
 * import java.io.File;
 * import java.nio.file.Files;
 * import java.nio.file.Paths;
 * 
 * public class App {
 *     public static void main(String[] args) {
 *         Pulumi.run(App::stack);
 *     }
 * 
 *     public static void stack(Context ctx) {
 *         // This is an example of an Email connection.
 *         var passwordlessEmail = new Connection("passwordlessEmail", ConnectionArgs.builder()        
 *             .strategy("email")
 *             .name("email")
 *             .options(ConnectionOptionsArgs.builder()
 *                 .name("email")
 *                 .from("{{ application.name }} \u003croot@auth0.com\u003e")
 *                 .subject("Welcome to {{ application.name }}")
 *                 .syntax("liquid")
 *                 .template("\u003chtml\u003eThis is the body of the email\u003c/html\u003e")
 *                 .disableSignup(false)
 *                 .bruteForceProtection(true)
 *                 .setUserRootAttributes("on_each_login")
 *                 .nonPersistentAttrs()
 *                 .authParams(Map.ofEntries(
 *                     Map.entry("scope", "openid email profile offline_access"),
 *                     Map.entry("response_type", "code")
 *                 ))
 *                 .totp(ConnectionOptionsTotpArgs.builder()
 *                     .timeStep(300)
 *                     .length(6)
 *                     .build())
 *                 .build())
 *             .build());
 * 
 *     }
 * }
 * }
 * \u003c!--End PulumiCodeChooser --\u003e
 * 
 * ### SAML Connection
 * 
 * \u003c!--Start PulumiCodeChooser --\u003e
 * {@code
 * package generated_program;
 * 
 * import com.pulumi.Context;
 * import com.pulumi.Pulumi;
 * import com.pulumi.core.Output;
 * import com.pulumi.auth0.Connection;
 * import com.pulumi.auth0.ConnectionArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsSigningKeyArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsDecryptionKeyArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsIdpInitiatedArgs;
 * import static com.pulumi.codegen.internal.Serialization.*;
 * import java.util.List;
 * import java.util.ArrayList;
 * import java.util.Map;
 * import java.io.File;
 * import java.nio.file.Files;
 * import java.nio.file.Paths;
 * 
 * public class App {
 *     public static void main(String[] args) {
 *         Pulumi.run(App::stack);
 *     }
 * 
 *     public static void stack(Context ctx) {
 *         // This is an example of a SAML connection.
 *         var samlp = new Connection("samlp", ConnectionArgs.builder()        
 *             .name("SAML-Connection")
 *             .strategy("samlp")
 *             .options(ConnectionOptionsArgs.builder()
 *                 .debug(false)
 *                 .signingCert("\u003csigning-certificate\u003e")
 *                 .signInEndpoint("https://saml.provider/sign_in")
 *                 .signOutEndpoint("https://saml.provider/sign_out")
 *                 .disableSignOut(true)
 *                 .tenantDomain("example.com")
 *                 .domainAliases(                
 *                     "example.com",
 *                     "alias.example.com")
 *                 .protocolBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST")
 *                 .requestTemplate("""
 * \u003csamlp:AuthnRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
 * @@AssertServiceURLAndDestination@@
 *     ID="@@ID@@"
 *     IssueInstant="@@IssueInstant@@"
 *     ProtocolBinding="@@ProtocolBinding@@" Version="2.0"\u003e
 *     \u003csaml:Issuer xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"\u003e@@Issuer@@\u003c/saml:Issuer\u003e
 * \u003c/samlp:AuthnRequest\u003e                """)
 *                 .userIdAttribute("https://saml.provider/imi/ns/identity-200810")
 *                 .signatureAlgorithm("rsa-sha256")
 *                 .digestAlgorithm("sha256")
 *                 .iconUrl("https://saml.provider/assets/logo.png")
 *                 .entityId("\u003centity_id\u003e")
 *                 .metadataXml("""
 *     \u003c?xml version="1.0"?\u003e
 *     \u003cmd:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" xmlns:ds="http://www.w3.org/2000/09/xmldsig#" entityID="https://example.com"\u003e
 *       \u003cmd:IDPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol"\u003e
 *         \u003cmd:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://saml.provider/sign_out"/\u003e
 *         \u003cmd:SingleSignOnService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://saml.provider/sign_in"/\u003e
 *       \u003c/md:IDPSSODescriptor\u003e
 *     \u003c/md:EntityDescriptor\u003e
 *                 """)
 *                 .metadataUrl("https://saml.provider/imi/ns/FederationMetadata.xml")
 *                 .fieldsMap(serializeJson(
 *                     jsonObject(
 *                         jsonProperty("name", jsonArray(
 *                             "name", 
 *                             "nameidentifier"
 *                         )),
 *                         jsonProperty("email", jsonArray(
 *                             "emailaddress", 
 *                             "nameidentifier"
 *                         )),
 *                         jsonProperty("family_name", "surname")
 *                     )))
 *                 .signingKey(ConnectionOptionsSigningKeyArgs.builder()
 *                     .key("""
 * -----BEGIN PRIVATE KEY-----
 * ...{your private key here}...
 * -----END PRIVATE KEY-----                    """)
 *                     .cert("""
 * -----BEGIN CERTIFICATE-----
 * ...{your public key cert here}...
 * -----END CERTIFICATE-----                    """)
 *                     .build())
 *                 .decryptionKey(ConnectionOptionsDecryptionKeyArgs.builder()
 *                     .key("""
 * -----BEGIN PRIVATE KEY-----
 * ...{your private key here}...
 * -----END PRIVATE KEY-----                    """)
 *                     .cert("""
 * -----BEGIN CERTIFICATE-----
 * ...{your public key cert here}...
 * -----END CERTIFICATE-----                    """)
 *                     .build())
 *                 .idpInitiated(ConnectionOptionsIdpInitiatedArgs.builder()
 *                     .clientId("client_id")
 *                     .clientProtocol("samlp")
 *                     .clientAuthorizeQuery("type=code\u0026timeout=30")
 *                     .build())
 *                 .build())
 *             .build());
 * 
 *     }
 * }
 * }
 * \u003c!--End PulumiCodeChooser --\u003e
 * 
 * ### WindowsLive Connection
 * 
 * \u003c!--Start PulumiCodeChooser --\u003e
 * {@code
 * package generated_program;
 * 
 * import com.pulumi.Context;
 * import com.pulumi.Pulumi;
 * import com.pulumi.core.Output;
 * import com.pulumi.auth0.Connection;
 * import com.pulumi.auth0.ConnectionArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
 * import java.util.List;
 * import java.util.ArrayList;
 * import java.util.Map;
 * import java.io.File;
 * import java.nio.file.Files;
 * import java.nio.file.Paths;
 * 
 * public class App {
 *     public static void main(String[] args) {
 *         Pulumi.run(App::stack);
 *     }
 * 
 *     public static void stack(Context ctx) {
 *         // This is an example of a WindowsLive connection.
 *         var windowslive = new Connection("windowslive", ConnectionArgs.builder()        
 *             .name("Windowslive-Connection")
 *             .strategy("windowslive")
 *             .options(ConnectionOptionsArgs.builder()
 *                 .clientId("\u003cclient-id\u003e")
 *                 .clientSecret("\u003cclient-secret\u003e")
 *                 .strategyVersion(2)
 *                 .scopes(                
 *                     "signin",
 *                     "graph_user")
 *                 .setUserRootAttributes("on_first_login")
 *                 .nonPersistentAttrs(                
 *                     "ethnicity",
 *                     "gender")
 *                 .build())
 *             .build());
 * 
 *     }
 * }
 * }
 * \u003c!--End PulumiCodeChooser --\u003e
 * 
 * ### OIDC Connection
 * 
 * \u003c!--Start PulumiCodeChooser --\u003e
 * {@code
 * package generated_program;
 * 
 * import com.pulumi.Context;
 * import com.pulumi.Pulumi;
 * import com.pulumi.core.Output;
 * import com.pulumi.auth0.Connection;
 * import com.pulumi.auth0.ConnectionArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsConnectionSettingsArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsAttributeMapArgs;
 * import static com.pulumi.codegen.internal.Serialization.*;
 * import java.util.List;
 * import java.util.ArrayList;
 * import java.util.Map;
 * import java.io.File;
 * import java.nio.file.Files;
 * import java.nio.file.Paths;
 * 
 * public class App {
 *     public static void main(String[] args) {
 *         Pulumi.run(App::stack);
 *     }
 * 
 *     public static void stack(Context ctx) {
 *         // This is an example of an OIDC connection.
 *         var oidc = new Connection("oidc", ConnectionArgs.builder()        
 *             .name("oidc-connection")
 *             .displayName("OIDC Connection")
 *             .strategy("oidc")
 *             .showAsButton(false)
 *             .options(ConnectionOptionsArgs.builder()
 *                 .clientId("1234567")
 *                 .clientSecret("1234567")
 *                 .domainAliases("example.com")
 *                 .tenantDomain("")
 *                 .iconUrl("https://example.com/assets/logo.png")
 *                 .type("back_channel")
 *                 .issuer("https://www.paypalobjects.com")
 *                 .jwksUri("https://api.paypal.com/v1/oauth2/certs")
 *                 .discoveryUrl("https://www.paypalobjects.com/.well-known/openid-configuration")
 *                 .tokenEndpoint("https://api.paypal.com/v1/oauth2/token")
 *                 .userinfoEndpoint("https://api.paypal.com/v1/oauth2/token/userinfo")
 *                 .authorizationEndpoint("https://www.paypal.com/signin/authorize")
 *                 .scopes(                
 *                     "openid",
 *                     "email")
 *                 .setUserRootAttributes("on_first_login")
 *                 .nonPersistentAttrs(                
 *                     "ethnicity",
 *                     "gender")
 *                 .connectionSettings(ConnectionOptionsConnectionSettingsArgs.builder()
 *                     .pkce("auto")
 *                     .build())
 *                 .attributeMap(ConnectionOptionsAttributeMapArgs.builder()
 *                     .mappingMode("use_map")
 *                     .userinfoScope("openid email profile groups")
 *                     .attributes(serializeJson(
 *                         jsonObject(
 *                             jsonProperty("name", "${context.tokenset.name}"),
 *                             jsonProperty("email", "${context.tokenset.email}"),
 *                             jsonProperty("email_verified", "${context.tokenset.email_verified}"),
 *                             jsonProperty("nickname", "${context.tokenset.nickname}"),
 *                             jsonProperty("picture", "${context.tokenset.picture}"),
 *                             jsonProperty("given_name", "${context.tokenset.given_name}"),
 *                             jsonProperty("family_name", "${context.tokenset.family_name}")
 *                         )))
 *                     .build())
 *                 .build())
 *             .build());
 * 
 *     }
 * }
 * }
 * \u003c!--End PulumiCodeChooser --\u003e
 * 
 * ### Okta Connection
 * 
 * \u003c!--Start PulumiCodeChooser --\u003e
 * {@code
 * package generated_program;
 * 
 * import com.pulumi.Context;
 * import com.pulumi.Pulumi;
 * import com.pulumi.core.Output;
 * import com.pulumi.auth0.Connection;
 * import com.pulumi.auth0.ConnectionArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsConnectionSettingsArgs;
 * import com.pulumi.auth0.inputs.ConnectionOptionsAttributeMapArgs;
 * import static com.pulumi.codegen.internal.Serialization.*;
 * import java.util.List;
 * import java.util.ArrayList;
 * import java.util.Map;
 * import java.io.File;
 * import java.nio.file.Files;
 * import java.nio.file.Paths;
 * 
 * public class App {
 *     public static void main(String[] args) {
 *         Pulumi.run(App::stack);
 *     }
 * 
 *     public static void stack(Context ctx) {
 *         // This is an example of an Okta Workforce connection.
 *         var okta = new Connection("okta", ConnectionArgs.builder()        
 *             .name("okta-connection")
 *             .displayName("Okta Workforce Connection")
 *             .strategy("okta")
 *             .showAsButton(false)
 *             .options(ConnectionOptionsArgs.builder()
 *                 .clientId("1234567")
 *                 .clientSecret("1234567")
 *                 .domain("example.okta.com")
 *                 .domainAliases("example.com")
 *                 .issuer("https://example.okta.com")
 *                 .jwksUri("https://example.okta.com/oauth2/v1/keys")
 *                 .tokenEndpoint("https://example.okta.com/oauth2/v1/token")
 *                 .userinfoEndpoint("https://example.okta.com/oauth2/v1/userinfo")
 *                 .authorizationEndpoint("https://example.okta.com/oauth2/v1/authorize")
 *                 .scopes(                
 *                     "openid",
 *                     "email")
 *                 .setUserRootAttributes("on_first_login")
 *                 .nonPersistentAttrs(                
 *                     "ethnicity",
 *                     "gender")
 *                 .upstreamParams(serializeJson(
 *                     jsonObject(
 *                         jsonProperty("screen_name", jsonObject(
 *                             jsonProperty("alias", "login_hint")
 *                         ))
 *                     )))
 *                 .connectionSettings(ConnectionOptionsConnectionSettingsArgs.builder()
 *                     .pkce("auto")
 *                     .build())
 *                 .attributeMap(ConnectionOptionsAttributeMapArgs.builder()
 *                     .mappingMode("basic_profile")
 *                     .userinfoScope("openid email profile groups")
 *                     .attributes(serializeJson(
 *                         jsonObject(
 *                             jsonProperty("name", "${context.tokenset.name}"),
 *                             jsonProperty("email", "${context.tokenset.email}"),
 *                             jsonProperty("email_verified", "${context.tokenset.email_verified}"),
 *                             jsonProperty("nickname", "${context.tokenset.nickname}"),
 *                             jsonProperty("picture", "${context.tokenset.picture}"),
 *                             jsonProperty("given_name", "${context.tokenset.given_name}"),
 *                             jsonProperty("family_name", "${context.tokenset.family_name}")
 *                         )))
 *                     .build())
 *                 .build())
 *             .build());
 * 
 *     }
 * }
 * }
 * \u003c!--End PulumiCodeChooser --\u003e
 * 
 * ## Import
 * 
 * This resource can be imported by specifying the connection ID.
 * 
 * # 
 * 
 * Example:
 * 
 * ```sh
 * $ pulumi import auth0:index/connection:Connection google &#34;con_a17f21fdb24d48a0&#34;
 * ```
 * 