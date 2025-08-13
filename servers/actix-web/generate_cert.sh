#!/bin/bash

# Generate private key
openssl genpkey -algorithm RSA -out server.key -pkeyopt rsa_keygen_bits:2048

# Generate config for SAN
cat > san.cnf <<EOF
[req]
distinguished_name=req
[san]
subjectAltName=DNS:localhost,IP:127.0.0.1
EOF

# Generate CSR
openssl req -new -key server.key -out server.csr -subj "/CN=localhost" -config san.cnf

# Generate self-signed certificate with v3 extensions
openssl x509 -req -days 365 -in server.csr -signkey server.key -out server.crt -extensions san -extfile san.cnf

# Clean up
rm server.csr san.cnf

# Set permissions
chmod 600 server.key
chmod 644 server.crt 