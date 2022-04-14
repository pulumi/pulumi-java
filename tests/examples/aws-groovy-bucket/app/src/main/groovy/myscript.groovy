import io.pulumi.*
import io.pulumi.core.*
import io.pulumi.aws.s3.*
import io.pulumi.aws.ssm.*
import io.pulumi.deployment.Deployment

Pulumi.run { ctx ->
    bucket = new Bucket("mybucket")
    ctx.export("Hello", Output.of("World!"))
    ctx.export("bucketname", bucket.bucket)
    ctx.exports()
}
