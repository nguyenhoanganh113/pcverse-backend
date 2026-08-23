DO
$$
BEGIN
    IF to_regclass('public.category_attribute') IS NOT NULL
        AND to_regclass('public.category_attributes') IS NOT NULL THEN
        RAISE EXCEPTION
            'Both category_attribute and category_attributes exist; merge the data before continuing';
    END IF;

    IF to_regclass('public.category_attribute') IS NOT NULL THEN
        ALTER TABLE public.category_attribute
            RENAME TO category_attributes;
    END IF;
END
$$;
