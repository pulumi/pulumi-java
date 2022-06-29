resource logs "aws:s3:Bucket" {}

resource bucket "aws:s3:Bucket" {
	loggings = [{
		targetBucket = logs.bucket,
	}]
}

resource indexFile "aws:s3:BucketObject" {
	bucket = bucket.id
	source = readFile("./index.html")
}

output targetBucket {
	value = bucket.loggings[0].targetBucket
}
