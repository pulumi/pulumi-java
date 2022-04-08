# Resource Init Design

The field initialization for Custom resources is unexpected. This doc
documents the unusual design.

The class hierarchy is as follows:

```
class Resource
class CustomResource extends Resource
class MyUserResource extends CustomResource
```

Note that there are thousands of classes that extend `CustomResource`
and they are usually all auto-generated in SDKs pertaining to a
particular resource provider, such as `pulumi-aws`,
`pulumi-azure-native`, or user-generated components. The code for
these is auto-generated.

Because there are so many, the less custom code we emit for them, the
smaller the SDKs are and the faster everything works. Current design
favors reflection to do field init, over generating more init code in
the subclasses.

Precisely, these are the fields that need init:

- Output<String> Resource.urn
- Output<String> CustomResource.id
- MyUserResource.{f1,f2...} Output-typed custom fields

There are two levels of init. For this discussion, we can think of
`Output` as a kind of `Promise`. First, the fields must be set to a
non-null but yet-incomplete `Promise`. Second, the `Promise` must
complete either normally or exceptionally when the information becomes
available.

In the Pulumi programming model, both `id`, `urn`, and all the
`Output` custom field values become known only eventually, after the
engine and resource providers do their job, potentially deploying
cloud resources.

How it is implemented: `Resource` constructor (top of the class
hierarchy) does two things:

1. Inits all these fields to non-nil incomplete promise values, using
   reflection when necessary to discover subclass-defined fields on
   the current object

2. Starts a background async computation `readOrRegisterResource` that
   must complete all the promises

As a consequence of this design, we have a limitation
[#314](https://github.com/pulumi/pulumi-jvm/issues/314): subclasses
currently cannot reliably create and instantiate additional fields. If
they do, the field initializers may not be run in time for the SDK to
observe a fully constructed resource.

It is currently understood that users never need to manually create
`Custom` resource classes, therefore the limitation may remain
unaddressed.
