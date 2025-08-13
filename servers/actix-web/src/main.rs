use std::io;
use std::fs::File;
use std::io::BufReader;
use std::time::Duration;

use actix_http::{HttpService, Request, Response};
use actix_server::Server;
//use actix_utils::future::ok;
use actix_service::fn_service;
use rustls::{ServerConfig, pki_types::{CertificateDer, PrivateKeyDer}};
use rustls_pemfile::{certs, pkcs8_private_keys};
use actix_rt::System;

fn main() -> io::Result<()> {
    println!("Started Actix-web server on 0.0.0.0:443");
    System::new().block_on(async move {
        let config = load_rustls_config();
        Server::build()
            .bind("tls", ("0.0.0.0", 443), move || {
                HttpService::build()
                    .finish(fn_service(|_req: Request| async {
                        tokio::time::sleep(Duration::from_millis(50)).await;
                        Ok::<_, actix_http::Error>(Response::ok().set_body("hello world!"))
                    }))
                    .rustls_0_23(config.clone())
            })?
            .run()
            .await
    })
}

fn load_rustls_config() -> ServerConfig {
    let cert_file = &mut BufReader::new(File::open("server.crt").expect("cannot open certificate"));
    let key_file = &mut BufReader::new(File::open("server.key").expect("cannot open private key"));

    let cert_chain = certs(cert_file)
        .map(|c| c.expect("invalid cert format").into())
        .collect::<Vec<CertificateDer>>();
    let mut keys = pkcs8_private_keys(key_file)
        .map(|k| k.expect("invalid key format").into())
        .collect::<Vec<PrivateKeyDer>>();

    ServerConfig::builder()
        .with_no_client_auth()
        .with_single_cert(cert_chain, keys.remove(0))
        .expect("bad certificate/key")
}
