#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ReasonCode {
    PayloadRequired,
    RequestBodyInvalid,
    RequestParameterMissing,
    RequestParameterInvalid,
    ValidationError,
    PatientIdRequired,
    NucleoPatientEmpty,
    NucleoStaffEmpty,
    StaffUserIdRequired,
    RoleRequired,
    AvaliacaoPacienteIdRequired,
    TituloAvaliacaoRequired,
    InboundRequiredField,
    InboundParseError,
    UnsupportedRoutingKey,
    AuthenticationRequired,
    AuthorizationDenied,
    AcolhimentoNotFound,
    NucleoPatientNotFound,
    AcolhimentoAvaliacaoNotFound,
    AcolhimentoAlreadyExists,
    AcolhimentoAvaliacaoAlreadyExists,
    DuplicateEvent,
    UnsupportedEventVersion,
    DatabaseAccessFailed,
    UnexpectedError,
    IntegrationFailure,
    PersistenceFailure,
    RouteNotFound,
    MethodNotAllowed,
    PayloadTooLarge,
    RequestTimeout,
    MessagingUnavailable,
}

impl ReasonCode {
    pub const fn code(self) -> &'static str {
        match self {
            Self::PayloadRequired => "PAYLOAD_REQUIRED",
            Self::RequestBodyInvalid => "REQUEST_BODY_INVALID",
            Self::RequestParameterMissing => "REQUEST_PARAMETER_MISSING",
            Self::RequestParameterInvalid => "REQUEST_PARAMETER_INVALID",
            Self::ValidationError => "VALIDATION_ERROR",
            Self::PatientIdRequired => "PATIENT_ID_REQUIRED",
            Self::NucleoPatientEmpty => "NUCLEO_PATIENT_EMPTY",
            Self::NucleoStaffEmpty => "NUCLEO_STAFF_EMPTY",
            Self::StaffUserIdRequired => "STAFF_USER_ID_REQUIRED",
            Self::RoleRequired => "ROLE_REQUIRED",
            Self::AvaliacaoPacienteIdRequired => "AVALIACAO_PACIENTE_ID_REQUIRED",
            Self::TituloAvaliacaoRequired => "TITULO_AVALIACAO_REQUIRED",
            Self::InboundRequiredField => "INBOUND_REQUIRED_FIELD",
            Self::InboundParseError => "INBOUND_PARSE_ERROR",
            Self::UnsupportedRoutingKey => "UNSUPPORTED_ROUTING_KEY",
            Self::AuthenticationRequired => "AUTHENTICATION_REQUIRED",
            Self::AuthorizationDenied => "AUTHORIZATION_DENIED",
            Self::AcolhimentoNotFound => "ACOLHIMENTO_NOT_FOUND",
            Self::NucleoPatientNotFound => "NUCLEO_PATIENT_NOT_FOUND",
            Self::AcolhimentoAvaliacaoNotFound => "ACOLHIMENTO_AVALIACAO_NOT_FOUND",
            Self::AcolhimentoAlreadyExists => "ACOLHIMENTO_ALREADY_EXISTS",
            Self::AcolhimentoAvaliacaoAlreadyExists => "ACOLHIMENTO_AVALIACAO_ALREADY_EXISTS",
            Self::DuplicateEvent => "DUPLICATE_EVENT",
            Self::UnsupportedEventVersion => "UNSUPPORTED_EVENT_VERSION",
            Self::DatabaseAccessFailed => "DATABASE_ACCESS_FAILED",
            Self::UnexpectedError => "UNEXPECTED_ERROR",
            Self::IntegrationFailure => "INTEGRATION_FAILURE",
            Self::PersistenceFailure => "PERSISTENCE_FAILURE",
            Self::RouteNotFound => "ROUTE_NOT_FOUND",
            Self::MethodNotAllowed => "METHOD_NOT_ALLOWED",
            Self::PayloadTooLarge => "PAYLOAD_TOO_LARGE",
            Self::RequestTimeout => "REQUEST_TIMEOUT",
            Self::MessagingUnavailable => "MESSAGING_UNAVAILABLE",
        }
    }

    pub const fn message(self) -> &'static str {
        match self {
            Self::PayloadRequired => "Payload obrigatorio",
            Self::RequestBodyInvalid => "Corpo da requisicao invalido",
            Self::RequestParameterMissing => "Parametro da requisicao ausente",
            Self::RequestParameterInvalid => "Parametro da requisicao invalido",
            Self::ValidationError => "Falha de validacao do payload/evento.",
            Self::PatientIdRequired => "Patient id obrigatorio",
            Self::NucleoPatientEmpty => "Lista de nucleos do paciente vazia",
            Self::NucleoStaffEmpty => "Lista de staff do nucleo vazia",
            Self::StaffUserIdRequired => "Staff user id obrigatorio",
            Self::RoleRequired => "Role do staff obrigatoria",
            Self::AvaliacaoPacienteIdRequired => "Avaliacao paciente id obrigatorio",
            Self::TituloAvaliacaoRequired => "Titulo da avaliacao obrigatorio",
            Self::InboundRequiredField => "Campo obrigatorio ausente no inbound.",
            Self::InboundParseError => "Falha ao parsear mensagem inbound.",
            Self::UnsupportedRoutingKey => "Routing key nao suportada.",
            Self::AuthenticationRequired => "Autenticacao obrigatoria",
            Self::AuthorizationDenied => "Usuario nao autorizado",
            Self::AcolhimentoNotFound => "Acolhimento nao encontrado",
            Self::NucleoPatientNotFound => "Nucleo do paciente nao encontrado",
            Self::AcolhimentoAvaliacaoNotFound => "Avaliacao de acolhimento nao encontrada",
            Self::AcolhimentoAlreadyExists => "Acolhimento ja cadastrado para o paciente",
            Self::AcolhimentoAvaliacaoAlreadyExists => {
                "Avaliacao ja vinculada ao acolhimento do paciente"
            }
            Self::DuplicateEvent => "Evento duplicado ja processado.",
            Self::UnsupportedEventVersion => "Versao de evento nao suportada.",
            Self::DatabaseAccessFailed => "Falha de acesso ao banco de dados",
            Self::UnexpectedError => "Erro inesperado",
            Self::IntegrationFailure => "Falha de integracao com dependencia externa.",
            Self::PersistenceFailure => "Falha de persistencia no banco de dados.",
            Self::RouteNotFound => "Rota nao encontrada",
            Self::MethodNotAllowed => "Metodo nao permitido",
            Self::PayloadTooLarge => "Payload excede o limite permitido",
            Self::RequestTimeout => "Tempo limite da requisicao excedido",
            Self::MessagingUnavailable => "Mensageria indisponivel",
        }
    }

    pub const fn is_retryable(self) -> bool {
        matches!(
            self,
            Self::DatabaseAccessFailed | Self::IntegrationFailure | Self::PersistenceFailure
        )
    }
}
