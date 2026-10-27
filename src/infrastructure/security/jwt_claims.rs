use super::authenticated_user::AuthenticatedUser;
use serde::Deserialize;
use std::collections::HashSet;

#[derive(Deserialize)]
pub(crate) struct JwtClaims {
    sub: String,
    #[serde(default)]
    azp: Option<String>,
    #[serde(default)]
    realm_access: serde_json::Value,
}

impl JwtClaims {
    pub fn subject_is_empty(&self) -> bool {
        self.sub.trim().is_empty()
    }

    pub fn into_authenticated_user(self) -> AuthenticatedUser {
        let roles: HashSet<String> = self
            .realm_access
            .get("roles")
            .and_then(serde_json::Value::as_array)
            .into_iter()
            .flatten()
            .filter_map(|role| role.as_str())
            .map(str::trim)
            .filter(|role| !role.is_empty())
            .map(|role| role.strip_prefix("ROLE_").unwrap_or(role).to_owned())
            .collect();
        AuthenticatedUser::new(self.sub, self.azp, roles)
    }
}
