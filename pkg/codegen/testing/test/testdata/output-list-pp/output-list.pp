resource pet "random:index/randomPet:RandomPet" {}

# A list whose elements are all plain values still generates plain varargs.
resource plainList "random:index/randomShuffle:RandomShuffle" {
    inputs = ["a", "b"]
}

# A list mixing outputs and plain values has no matching builder overload as varargs,
# so it has to be combined with Output.all().
resource mixedList "random:index/randomShuffle:RandomShuffle" {
    inputs = [
        pet.id,
        "${pet.id}-suffix",
        "literal"
    ]
}

# A single-element list is generated the same way: the element is still an output.
resource singleOutput "random:index/randomShuffle:RandomShuffle" {
    inputs = [pet.id]
}

# The same applies to a list nested inside invoke arguments, which is what makes the
# S3 bucket policy example in the registry docs fail to compile.
resource sourceBucket "infra:index:Bucket" {}

policyDocument = invoke("infra:index:getPolicyDocument", {
    statements = [{
        actions = ["s3:GetObject"]
        resources = [
            sourceBucket.bucket,
            "${sourceBucket.bucket}/*"
        ]
    }]
})
