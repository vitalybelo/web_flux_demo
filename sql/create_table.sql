CREATE TABLE public.callback_table (
                                       id uuid NOT NULL,
                                       user_id varchar(36) NOT NULL,
                                       callback_type varchar NULL,
                                       callback_json text NULL,
                                       "timestamp" int8 NULL,
                                       CONSTRAINT callback_table_pk PRIMARY KEY (id)
);
CREATE INDEX callback_table_user_id_idx ON public.callback_table USING btree (user_id);