use std::convert::Infallible;
use hyper::{Body, Method, Request, Response, Server, StatusCode};
use hyper::service::{make_service_fn, service_fn};
use sqlx::MySqlPool;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    // Read DATABASE_URL from env or fallback to default
    let database_url = std::env::var("DATABASE_URL")
        .unwrap_or_else(|_| "mysql://testuser:testpassword@mysql:3306/testdb".to_string());
    let pool = MySqlPool::connect(&database_url).await?;

    let addr = ([0, 0, 0, 0], 80).into();
    let make_svc = make_service_fn(move |_| {
        let pool = pool.clone();
        async move {
            Ok::<_, Infallible>(service_fn(move |req: Request<Body>| {
                let pool = pool.clone();
                async move {
                    match (req.method(), req.uri().path()) {
                        (&Method::GET, "/incr") => {
                            if let Err(err) = do_increment(&pool).await {
                                let resp = Response::builder()
                                    .status(StatusCode::INTERNAL_SERVER_ERROR)
                                    .body(Body::from(format!("Error: {}", err)))
                                    .unwrap();
                                return Ok::<_, Infallible>(resp);
                            }
                            Ok(Response::new(Body::from("OK")))
                        }
                        (&Method::GET, "/check") => {
                            match do_check(&pool).await {
                                Ok(count) => Ok(Response::new(Body::from(count.to_string()))),
                                Err(err) => {
                                    let resp = Response::builder()
                                        .status(StatusCode::INTERNAL_SERVER_ERROR)
                                        .body(Body::from(format!("Error: {}", err)))
                                        .unwrap();
                                    Ok(resp)
                                }
                            }
                        }
                        _ => {
                            let resp = Response::builder()
                                .status(StatusCode::NOT_FOUND)
                                .body(Body::from("Not Found"))
                                .unwrap();
                            Ok(resp)
                        }
                    }
                }
            }))
        }
    });

    println!("Server listening on http://{}", addr);
    Server::bind(&addr).serve(make_svc).await?;
    Ok(())
}

async fn do_increment(pool: &MySqlPool) -> Result<(), sqlx::Error> {
    sqlx::query("UPDATE test SET counter = counter + 1 WHERE id = 1")
        .execute(pool)
        .await?;
    Ok(())
}

async fn do_check(pool: &MySqlPool) -> Result<i64, sqlx::Error> {
    let (count,): (i64,) = sqlx::query_as("SELECT counter FROM test WHERE id = 1")
        .fetch_one(pool)
        .await?;
    Ok(count)
}
