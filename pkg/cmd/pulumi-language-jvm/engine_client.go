package main

import (
	"context"
	"fmt"
	"io"

	empty "github.com/golang/protobuf/ptypes/empty"
	grpc "google.golang.org/grpc"

	"github.com/pulumi/pulumi/sdk/v3/go/common/util/rpcutil"
	pulumirpc "github.com/pulumi/pulumi/sdk/v3/proto/go"
)

type engineClient struct {
	inner       pulumirpc.EngineClient
	innerCloser io.Closer
}

func newEngineClient(engineAddress string) (*engineClient, error) {
	// Make a connection to the real engine that we will log messages to.
	conn, err := grpc.Dial(
		engineAddress,
		grpc.WithInsecure(),
		rpcutil.GrpcChannelOptions(),
	)

	if err != nil {
		return nil, fmt.Errorf("language host could not make connection to engine: %w", err)
	}

	// Make a client around that connection.
	// We can then make our own server that will act as a
	// monitor for the SDK and forward to the real monitor.
	c := pulumirpc.NewEngineClient(conn)

	return &engineClient{
		inner:       c,
		innerCloser: conn,
	}, nil
}

var _ pulumirpc.EngineClient = &engineClient{}

var _ io.Closer = &engineClient{}

func (ec *engineClient) Log(
	ctx context.Context,
	in *pulumirpc.LogRequest,
	opts ...grpc.CallOption,
) (*empty.Empty, error) {
	return ec.inner.Log(ctx, in, opts...)
}

func (ec *engineClient) GetRootResource(
	ctx context.Context,
	in *pulumirpc.GetRootResourceRequest,
	opts ...grpc.CallOption,
) (*pulumirpc.GetRootResourceResponse, error) {
	return ec.inner.GetRootResource(ctx, in, opts...)
}

func (ec *engineClient) SetRootResource(
	ctx context.Context,
	in *pulumirpc.SetRootResourceRequest,
	opts ...grpc.CallOption,
) (*pulumirpc.SetRootResourceResponse, error) {
	return ec.inner.SetRootResource(ctx, in, opts...)
}

func (ec *engineClient) Close() error {
	return ec.innerCloser.Close()
}
