create extension if not exists "pgcrypto";

create table if not exists public.users (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null default '',
  email text not null default '',
  created_at timestamptz not null default now()
);

create table if not exists public.contacts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  full_name text not null,
  company text not null default '',
  job_title text not null default '',
  email text not null default '',
  phone text not null default '',
  website text not null default '',
  address text not null default '',
  notes text not null default '',
  date_met timestamptz,
  location_met text not null default '',
  status text not null default 'New',
  card_image_url text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint contacts_status_check check (
    status in ('New', 'Follow-up Needed', 'Contacted', 'Meeting Scheduled', 'Converted', 'Archived')
  )
);

create table if not exists public.tags (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  color text not null default '#2563EB',
  created_at timestamptz not null default now(),
  unique(user_id, name)
);

create table if not exists public.contact_tags (
  id uuid primary key default gen_random_uuid(),
  contact_id uuid not null references public.contacts(id) on delete cascade,
  tag_id uuid not null references public.tags(id) on delete cascade,
  unique(contact_id, tag_id)
);

create table if not exists public.follow_ups (
  id uuid primary key default gen_random_uuid(),
  contact_id uuid not null references public.contacts(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  due_date timestamptz not null,
  completed boolean not null default false,
  completed_at timestamptz,
  created_at timestamptz not null default now()
);

create table if not exists public.contact_activities (
  id uuid primary key default gen_random_uuid(),
  contact_id uuid not null references public.contacts(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  type text not null,
  description text not null default '',
  created_at timestamptz not null default now()
);

alter table public.users enable row level security;
alter table public.contacts enable row level security;
alter table public.tags enable row level security;
alter table public.contact_tags enable row level security;
alter table public.follow_ups enable row level security;
alter table public.contact_activities enable row level security;

grant usage on schema public to authenticated;
grant select, insert, update, delete on public.users to authenticated;
grant select, insert, update, delete on public.contacts to authenticated;
grant select, insert, update, delete on public.tags to authenticated;
grant select, insert, update, delete on public.contact_tags to authenticated;
grant select, insert, update, delete on public.follow_ups to authenticated;
grant select, insert, update, delete on public.contact_activities to authenticated;

create policy "Users can read own profile"
  on public.users for select
  to authenticated
  using (id = auth.uid());

create policy "Users can insert own profile"
  on public.users for insert
  to authenticated
  with check (id = auth.uid());

create policy "Users can update own profile"
  on public.users for update
  to authenticated
  using (id = auth.uid())
  with check (id = auth.uid());

create policy "Users can manage own contacts"
  on public.contacts for all
  to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

create policy "Users can manage own tags"
  on public.tags for all
  to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

create policy "Users can manage tags for own contacts"
  on public.contact_tags for all
  to authenticated
  using (
    exists (
      select 1
      from public.contacts c
      where c.id = contact_id and c.user_id = auth.uid()
    )
    and exists (
      select 1
      from public.tags t
      where t.id = tag_id and t.user_id = auth.uid()
    )
  )
  with check (
    exists (
      select 1
      from public.contacts c
      where c.id = contact_id and c.user_id = auth.uid()
    )
    and exists (
      select 1
      from public.tags t
      where t.id = tag_id and t.user_id = auth.uid()
    )
  );

create policy "Users can manage own follow ups"
  on public.follow_ups for all
  to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

create policy "Users can manage own activities"
  on public.contact_activities for all
  to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

insert into storage.buckets (id, name, public)
values ('card-images', 'card-images', false)
on conflict (id) do nothing;

create policy "Users can read own card images"
  on storage.objects for select
  to authenticated
  using (bucket_id = 'card-images' and owner = auth.uid());

create policy "Users can upload own card images"
  on storage.objects for insert
  to authenticated
  with check (bucket_id = 'card-images' and owner = auth.uid());

create policy "Users can update own card images"
  on storage.objects for update
  to authenticated
  using (bucket_id = 'card-images' and owner = auth.uid())
  with check (bucket_id = 'card-images' and owner = auth.uid());

create policy "Users can delete own card images"
  on storage.objects for delete
  to authenticated
  using (bucket_id = 'card-images' and owner = auth.uid());
