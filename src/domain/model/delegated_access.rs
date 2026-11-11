#[derive(Clone)]
pub struct DelegatedAccess(zeroize::Zeroizing<String>);

impl DelegatedAccess {
    pub fn new(token: String) -> Self {
        Self(zeroize::Zeroizing::new(token))
    }

    pub fn token(&self) -> &str {
        &self.0
    }
}
