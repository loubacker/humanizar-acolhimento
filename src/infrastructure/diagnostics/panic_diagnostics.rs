pub fn install_panic_hook() {
    std::panic::set_hook(Box::new(|_| {
        eprintln!("humanizar-acolhimento: falha inesperada no processo")
    }));
}
