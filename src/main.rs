#![forbid(unsafe_code)]

use humanizar_acolhimento::infrastructure::server;

#[tokio::main]
async fn main() {
    humanizar_acolhimento::infrastructure::diagnostics::install_panic_hook();
    if let Err(error) = server::run().await {
        eprintln!("humanizar-acolhimento: {}", error.message());
        std::process::exit(1);
    }
}
