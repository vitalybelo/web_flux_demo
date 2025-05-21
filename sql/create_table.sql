CREATE TABLE public.callback_table (
                                       id uuid NOT NULL,
                                       correlation_id varchar(36) NOT NULL,
                                       callback_type varchar NULL,
                                       callback_json text NULL,
                                       "timestamp" int8 NULL,
                                       CONSTRAINT newtable_pk PRIMARY KEY (id)
);
CREATE INDEX newtable_user_id_idx ON public.callback_table USING btree (correlation_id);