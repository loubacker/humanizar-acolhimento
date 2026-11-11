#![forbid(unsafe_code)]

// ===== Domain =====
pub mod domain {
    pub mod model {
        mod acolhimento;
        mod acolhimento_avaliacao;
        mod acolhimento_operation;
        mod anamnese_operation;
        mod avaliacao_paciente_reference;
        mod delegated_access;
        mod nucleo_patient;
        mod nucleo_patient_staff;

        pub use acolhimento::Acolhimento;
        pub use acolhimento_avaliacao::AcolhimentoAvaliacao;
        pub use acolhimento_operation::{
            OperationAction, OperationIdentity, OperationReservation, OperationState,
            OperationStatus,
        };
        pub use anamnese_operation::AnamneseOperation;
        pub use avaliacao_paciente_reference::AvaliacaoPacienteReference;
        pub use delegated_access::DelegatedAccess;
        pub use nucleo_patient::NucleoPatient;
        pub use nucleo_patient_staff::NucleoPatientStaff;

        pub mod messaging {
            mod acolhimento_retrieve_command;
            mod dead_letter_message;
            mod outbox_event;
            mod outbox_message;
            mod processed_event;

            pub use acolhimento_retrieve_command::{AcolhimentoRetrieveCommand, ProcessingOutcome};
            pub use dead_letter_message::DeadLetterMessage;
            pub use outbox_event::OutboxEvent;
            pub use outbox_message::OutboxMessage;
            pub use processed_event::ProcessedEvent;
        }

        pub mod enums {
            pub mod messaging {
                mod outbox_status;
                mod processed_result;

                pub use outbox_status::OutboxStatus;
                pub use processed_result::ProcessedResult;
            }

            mod reason_code;

            pub use reason_code::ReasonCode;
        }
    }

    pub mod exception {
        mod acolhimento_exception;
        mod persistence_exception;
        mod reason_code_exception;
        mod security_exception;
        mod technical_error;

        pub use acolhimento_exception::AcolhimentoException;
        pub use persistence_exception::PersistenceException;
        pub use reason_code_exception::ReasonCodeException;
        pub use security_exception::SecurityException;
        pub use technical_error::TechnicalError;
    }

    pub mod port {
        pub mod acolhimento {
            mod acolhimento_avaliacao_port;
            mod acolhimento_operation_port;
            mod acolhimento_port;
            mod avaliacao_paciente_reference_port;
            mod nucleo_patient_port;
            mod nucleo_patient_staff_port;

            pub use acolhimento_avaliacao_port::AcolhimentoAvaliacaoPort;
            pub use acolhimento_operation_port::AcolhimentoOperationPort;
            pub use acolhimento_port::AcolhimentoPort;
            pub use avaliacao_paciente_reference_port::AvaliacaoPacienteReferencePort;
            pub use nucleo_patient_port::NucleoPatientPort;
            pub use nucleo_patient_staff_port::NucleoPatientStaffPort;
        }

        pub mod messaging {
            mod dead_letter_quarantine_port;
            mod message_publisher_port;
            mod outbox_event_port;
            mod processed_event_port;

            pub use dead_letter_quarantine_port::DeadLetterQuarantinePort;
            pub use message_publisher_port::MessagePublisherPort;
            pub use outbox_event_port::OutboxEventPort;
            pub use processed_event_port::ProcessedEventPort;
        }

        pub use acolhimento::{
            AcolhimentoAvaliacaoPort, AcolhimentoOperationPort, AcolhimentoPort,
            AvaliacaoPacienteReferencePort, NucleoPatientPort, NucleoPatientStaffPort,
        };
    }
}

// ===== Application =====
pub mod application {
    pub mod catalog {
        mod aggregate_catalog;
        mod consumer_catalog;
        mod exchange_catalog;
        mod producer_catalog;
        mod queue_catalog;
        mod routing_key_catalog;

        pub use aggregate_catalog::AggregateCatalog;
        pub use consumer_catalog::ConsumerCatalog;
        pub use exchange_catalog::ExchangeCatalog;
        pub use producer_catalog::ProducerCatalog;
        pub use queue_catalog::QueueCatalog;
        pub use routing_key_catalog::RoutingKeyCatalog;
    }

    pub mod messaging {
        pub mod inbound {
            pub mod dto {
                mod inbound_envelope_dto;

                pub use inbound_envelope_dto::{AcolhimentoRetrieveCommandDto, InboundEnvelopeDto};
            }

            pub mod mapper {
                mod acolhimento_command_mapper;

                pub use acolhimento_command_mapper::AcolhimentoCommandMapper;
            }

            pub mod handler {
                mod acolhimento_retrieve_handler;

                pub use acolhimento_retrieve_handler::AcolhimentoRetrieveHandler;
            }
        }

        pub mod outbound {
            pub mod dto {
                mod outbound_envelope_dto;
                mod staff_notification_dto;

                pub use outbound_envelope_dto::{
                    AcolhimentoRejectedDto, AcolhimentoRetrievedDto, OutboundEnvelopeDto,
                };
                pub use staff_notification_dto::{StaffDesvinculadoDto, StaffVinculadoDto};
            }

            pub mod mapper {
                mod acolhimento_callback_mapper;

                pub use acolhimento_callback_mapper::AcolhimentoCallbackMapper;
            }

            pub mod factory {
                mod acolhimento_callback_factory;
                mod staff_outbox_factory;

                pub use acolhimento_callback_factory::AcolhimentoCallbackFactory;
                pub use staff_outbox_factory::StaffOutboxFactory;
            }
        }
    }

    pub mod http {
        pub mod dto {
            pub mod acolhimento {
                mod acolhimento_anamnese_dto;
                mod acolhimento_avaliacao_dto;
                mod acolhimento_dto;
                mod avaliacao_paciente_reference_dto;

                pub use acolhimento_anamnese_dto::{
                    AcolhimentoAnamneseCreateUpdateDto, AcolhimentoAnamneseRetrieveDto,
                    AnamneseDetailDto, AnamneseItemDto, AnamneseRequestDto, AnamneseSectionDto,
                };
                pub use acolhimento_avaliacao_dto::{
                    AcolhimentoAvaliacaoRegisterDto, AcolhimentoAvaliacaoRetrieveDto,
                    AvaliacaoDisponivelDto,
                };
                pub use acolhimento_dto::{
                    AcolhimentoCreateDto, AcolhimentoPatientStatusDto, AcolhimentoRetrieveDto,
                    AcolhimentoUpdateDto, NucleoPatientDto, NucleoPatientRetrieveDto,
                    NucleoStaffDto,
                };
                pub use avaliacao_paciente_reference_dto::{
                    AvaliacaoPacienteErrorEnvelopeDto, AvaliacaoPacienteReferenceDto,
                    AvaliacaoPacienteReferenceEnvelopeDto,
                };
            }

            pub use acolhimento::{
                AcolhimentoAnamneseCreateUpdateDto, AcolhimentoAnamneseRetrieveDto,
                AcolhimentoAvaliacaoRegisterDto, AcolhimentoAvaliacaoRetrieveDto,
                AcolhimentoCreateDto, AcolhimentoPatientStatusDto, AcolhimentoRetrieveDto,
                AcolhimentoUpdateDto, AnamneseDetailDto, AnamneseItemDto, AnamneseRequestDto,
                AnamneseSectionDto, AvaliacaoDisponivelDto, AvaliacaoPacienteErrorEnvelopeDto,
                AvaliacaoPacienteReferenceDto, AvaliacaoPacienteReferenceEnvelopeDto,
                NucleoPatientDto, NucleoPatientRetrieveDto, NucleoStaffDto,
            };
        }

        pub mod mapper {
            pub mod acolhimento {
                mod acolhimento_anamnese_mapper;
                mod acolhimento_avaliacao_mapper;
                mod acolhimento_mapper;
                mod avaliacao_paciente_reference_mapper;

                pub use acolhimento_anamnese_mapper::AcolhimentoAnamneseMapper;
                pub use acolhimento_avaliacao_mapper::AcolhimentoAvaliacaoMapper;
                pub use acolhimento_mapper::AcolhimentoMapper;
                pub use avaliacao_paciente_reference_mapper::AvaliacaoPacienteReferenceMapper;
            }

            pub use acolhimento::{
                AcolhimentoAnamneseMapper, AcolhimentoAvaliacaoMapper, AcolhimentoMapper,
                AvaliacaoPacienteReferenceMapper,
            };
        }
    }

    pub(crate) mod validator {
        mod acolhimento_validator;
        mod field_validator;

        pub(crate) use acolhimento_validator::AcolhimentoValidator;
        pub(crate) use field_validator::{required, required_uuid};
    }

    pub mod service {
        mod acolhimento_anamnese_service;
        mod acolhimento_avaliacao_service;
        mod acolhimento_service;

        pub use acolhimento_anamnese_service::AcolhimentoAnamneseService;
        pub use acolhimento_avaliacao_service::AcolhimentoAvaliacaoService;
        pub use acolhimento_service::AcolhimentoService;
    }
}

// ===== Infrastructure =====
pub mod infrastructure {
    pub mod server;

    pub mod messaging {
        mod messaging_services;

        pub use messaging_services::MessagingServices;

        pub mod inbound {
            pub(crate) mod idempotency {
                mod processed_event_guard;

                pub(crate) use processed_event_guard::ProcessedEventGuard;
            }

            pub mod rabbit {
                mod acolhimento_retrieve_consumer;
                mod dead_letter_consumer;
                mod dead_letter_delivery_mapper;
                mod rabbit_runtime;

                pub use rabbit_runtime::RabbitRuntime;
            }
        }

        pub mod outbound {
            pub mod rabbit {
                mod rabbit_outbox_publisher;

                pub use rabbit_outbox_publisher::RabbitOutboxPublisher;
            }

            pub mod outbox {
                mod outbox_event_processor;
                pub(crate) mod outbox_relay_worker;
                mod outbox_retry_policy;
                pub(crate) mod retention_worker;

                pub(crate) use outbox_event_processor::OutboxEventProcessor;
                pub use outbox_retry_policy::OutboxRetryPolicy;
            }
        }
    }

    pub mod config {
        pub mod rabbit {
            mod rabbit_acknowledgement_config;
            mod rabbit_connection_config;
            mod rabbit_exchange_config;
            mod rabbit_queue_config;
            mod rabbit_settings;

            pub(crate) mod binding {
                mod rabbit_acolhimento_binding_config;

                pub(crate) use rabbit_acolhimento_binding_config::RabbitAcolhimentoBindingConfig;
            }

            pub(crate) use rabbit_acknowledgement_config::RabbitAcknowledgementConfig;
            pub(crate) use rabbit_connection_config::{RabbitConnectionConfig, rabbit_error};
            pub(crate) use rabbit_exchange_config::RabbitExchangeConfig;
            pub(crate) use rabbit_queue_config::RabbitQueueConfig;
            pub use rabbit_settings::RabbitSettings;
        }

        mod avaliacao_paciente_config;
        mod database_config;
        mod environment_config;
        mod security_config;
        mod server_config;

        pub use avaliacao_paciente_config::AvaliacaoPacienteSettings;
        pub use database_config::{DatabaseConfig, DatabaseConfigError, DatabaseSettings};
        pub(crate) use environment_config::EnvironmentConfig;
        pub use security_config::{SecurityConfig, SecurityConfigError, SecuritySettings};
        pub use server_config::{ServerConfig, ServerConfigError};
    }

    pub(crate) mod adapter {
        mod acolhimento_adapter;
        mod acolhimento_avaliacao_adapter;
        mod acolhimento_operation_adapter;
        mod avaliacao_paciente_reference_adapter;
        mod dead_letter_quarantine_adapter;
        mod nucleo_patient_adapter;
        mod nucleo_patient_staff_adapter;
        mod outbox_event_adapter;
        mod processed_event_adapter;

        pub(crate) use acolhimento_adapter::AcolhimentoAdapter;
        pub(crate) use acolhimento_avaliacao_adapter::AcolhimentoAvaliacaoAdapter;
        pub(crate) use acolhimento_operation_adapter::AcolhimentoOperationAdapter;
        pub(crate) use avaliacao_paciente_reference_adapter::AvaliacaoPacienteReferenceAdapter;
        pub(crate) use dead_letter_quarantine_adapter::DeadLetterQuarantineAdapter;
        pub(crate) use nucleo_patient_adapter::NucleoPatientAdapter;
        pub(crate) use nucleo_patient_staff_adapter::NucleoPatientStaffAdapter;
        pub(crate) use outbox_event_adapter::OutboxEventAdapter;
        pub(crate) use processed_event_adapter::ProcessedEventAdapter;
    }

    pub(crate) mod persistence {
        pub(crate) mod mapper {
            mod messaging_entity_mapper;

            pub(crate) use messaging_entity_mapper::MessagingEntityMapper;
        }

        pub(crate) mod entity {
            pub(crate) mod messaging {
                mod outbox_event_entity;
                mod processed_event_entity;

                pub(crate) use outbox_event_entity::OutboxEventEntity;
                pub(crate) use processed_event_entity::ProcessedEventEntity;
            }

            pub(crate) mod acolhimento {
                mod acolhimento_avaliacao_entity;
                mod acolhimento_entity;
                mod nucleo_patient_entity;
                mod nucleo_patient_staff_entity;

                pub(crate) use acolhimento_avaliacao_entity::AcolhimentoAvaliacaoEntity;
                pub(crate) use acolhimento_entity::AcolhimentoEntity;
                pub(crate) use nucleo_patient_entity::NucleoPatientEntity;
                pub(crate) use nucleo_patient_staff_entity::NucleoPatientStaffEntity;
            }

            pub(crate) use acolhimento::{
                AcolhimentoAvaliacaoEntity, AcolhimentoEntity, NucleoPatientEntity,
                NucleoPatientStaffEntity,
            };
        }

        pub(crate) mod repository {
            pub(crate) mod acolhimento {
                mod acolhimento_avaliacao_repository;
                mod acolhimento_operation_repository;
                mod acolhimento_repository;
                mod nucleo_patient_repository;
                mod nucleo_patient_staff_repository;

                pub(crate) use acolhimento_avaliacao_repository::AcolhimentoAvaliacaoRepository;
                pub(crate) use acolhimento_operation_repository::AcolhimentoOperationRepository;
                pub(crate) use acolhimento_repository::AcolhimentoRepository;
                pub(crate) use nucleo_patient_repository::NucleoPatientRepository;
                pub(crate) use nucleo_patient_staff_repository::NucleoPatientStaffRepository;
            }

            pub(crate) mod messaging {
                mod dead_letter_quarantine_repository;
                mod outbox_event_repository;
                mod processed_event_repository;

                pub(crate) use dead_letter_quarantine_repository::DeadLetterQuarantineRepository;
                pub(crate) use outbox_event_repository::OutboxEventRepository;
                pub(crate) use processed_event_repository::ProcessedEventRepository;
            }

            pub(crate) use acolhimento::{
                AcolhimentoAvaliacaoRepository, AcolhimentoOperationRepository,
                AcolhimentoRepository, NucleoPatientRepository, NucleoPatientStaffRepository,
            };
            pub(crate) use messaging::{
                DeadLetterQuarantineRepository, OutboxEventRepository, ProcessedEventRepository,
            };
        }
    }

    pub(crate) mod handler {
        mod postgres_error_handler;

        pub(crate) use postgres_error_handler::database_error;
    }

    pub mod diagnostics {
        mod panic_diagnostics;
        mod safe_url;

        pub use panic_diagnostics::install_panic_hook;
        pub use safe_url::SafeUrl;
    }

    pub mod security {
        mod authenticated_user;
        mod authorization;
        mod jwks_cache;
        mod jwt_claims;
        mod jwt_validator;

        pub use authenticated_user::AuthenticatedUser;
        pub(crate) use authorization::{authenticate, require_administrator};
        pub(crate) use jwks_cache::{JwksCache, JwksHttpTimeouts};
        pub(crate) use jwt_validator::JwtValidator;
    }

    pub mod controller {
        pub mod extractor;
        mod http_observability;
        mod operation_guard;
        mod request_context;
        mod router;
        mod state;

        pub use request_context::RequestContext;
        pub use router::{create_router, create_router_with_limits};
        pub use state::ApplicationState;

        pub mod dto {
            mod error_response_dto;
            mod success_response_dto;

            pub use error_response_dto::ErrorResponseDto;
            pub(crate) use success_response_dto::SuccessObservation;
            pub use success_response_dto::SuccessResponseDto;
        }

        pub mod handler {
            mod http_error;

            pub(crate) use http_error::ErrorObservation;
            pub use http_error::{HttpError, status_for};
        }

        pub(crate) mod acolhimento {
            pub(crate) mod acolhimento_anamnese_controller;
            pub(crate) mod acolhimento_avaliacao_controller;
            pub(crate) mod acolhimento_controller;
            pub(crate) mod acolhimento_operation_controller;
        }
    }
}
