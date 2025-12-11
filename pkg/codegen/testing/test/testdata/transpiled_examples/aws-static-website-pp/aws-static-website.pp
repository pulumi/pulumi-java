resource siteBucket "aws-native:s3:Bucket" {
	__logicalName = "site-bucket"
	websiteConfiguration = {
		indexDocument = "index.html"
	}
	publicAccessBlockConfiguration = {
		blockPublicAcls = false
	}
	ownershipControls = {
		rules = [{
			objectOwnership = "ObjectWriter"
		}]
	}
}

resource indexHtml "aws:s3/bucketObject:BucketObject" {
	__logicalName = "index.html"
	bucket = siteBucket
	source = fileAsset("./www/index.html")
	acl = "public-read"
	contentType = "text/html"

	options {
		version = "5.16.2"
	}
}

resource faviconPng "aws:s3/bucketObject:BucketObject" {
	__logicalName = "favicon.png"
	bucket = siteBucket
	source = fileAsset("./www/favicon.png")
	acl = "public-read"
	contentType = "image/png"

	options {
		version = "5.16.2"
	}
}

resource defaultProvider "pulumi:providers:aws" {
	__logicalName = "defaultProvider"

	options {
		version = "5.16.2"
	}
}

output bucketName {
	__logicalName = "bucketName"
	value = siteBucket.bucketName
}

output websiteUrl {
	__logicalName = "websiteUrl"
	value = siteBucket.websiteUrl
}
