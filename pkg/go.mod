module github.com/pulumi/pulumi-java/pkg

go 1.17

replace github.com/Sirupsen/logrus => github.com/sirupsen/logrus v1.5.0

require (
	github.com/golang/protobuf v1.5.2
	github.com/hashicorp/hcl/v2 v2.3.0
	github.com/pkg/errors v0.9.1
	github.com/pulumi/pulumi/pkg/v3 v3.24.2-0.20220207111004-a1e18dae4dc0
	github.com/pulumi/pulumi/sdk/v3 v3.24.2-0.20220207111004-a1e18dae4dc0
	github.com/stretchr/testify v1.6.1
	google.golang.org/grpc v1.37.0
	gopkg.in/yaml.v3 v3.0.0-20200313102051-9f266ea9e77c
)
