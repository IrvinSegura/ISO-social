-- Nexus media bucket on Supabase Storage.
-- Run once in the Supabase dashboard: SQL Editor -> New query -> paste -> Run.
--
-- Auth: the app sends the user's Firebase ID token (Authentication -> Sign In / Providers ->
-- Third-party Auth -> Firebase, project id "iso-social"). Firebase tokens carry no "role" claim,
-- so Supabase runs these requests as the "anon" role; the policies below therefore check the
-- token itself: issuer and audience of our Firebase project, and the uid ("sub") as the folder.
-- Supabase's own anon key (no Firebase issuer) can never write.

-- 1. Bucket: public for reads (URLs work in Coil and ExoPlayer without headers),
--    50 MB per file and only the content types the app uploads
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'media', 'media', true, 52428800,
  array[
    'image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/heic', 'image/heif',
    'video/mp4', 'video/quicktime', 'video/3gpp', 'video/webm',
    'audio/mp4', 'audio/mpeg', 'audio/aac', 'audio/ogg', 'audio/wav', 'audio/x-m4a',
    'application/pdf', 'text/plain', 'text/csv', 'application/zip',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/vnd.ms-powerpoint',
    'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    'application/vnd.oasis.opendocument.text',
    'application/vnd.oasis.opendocument.spreadsheet',
    'application/vnd.oasis.opendocument.presentation'
  ]
)
on conflict (id) do update set
  public = excluded.public,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

-- 2. The request comes from a signed-in user of our Firebase project
create or replace function public.is_nexus_user() returns boolean
language sql stable as $$
  select coalesce(auth.jwt() ->> 'iss', '') = 'https://securetoken.google.com/iso-social'
     and coalesce(auth.jwt() ->> 'aud', '') = 'iso-social'
     and coalesce(auth.jwt() ->> 'sub', '') <> ''
$$;

-- 3. Paths the app writes, each one inside the uploader's own folder:
--      posts/{uid}/{postId}/images/image_[0-3].jpg | audio.m4a | files/{name}
--      ecos/{postId}/{uid}/{uuid}.jpg|m4a
--      stories/{uid}/{uuid}.jpg|mp4
create or replace function public.is_own_media_path(path text) returns boolean
language sql stable as $$
  select case split_part(path, '/', 1)
    when 'posts' then
      split_part(path, '/', 2) = auth.jwt() ->> 'sub'
      and path ~ '^posts/[^/]+/[A-Za-z0-9]{20}/(images/image_[0-3]\.jpg|audio\.m4a|files/[A-Za-z0-9._ -]{1,100})$'
    when 'ecos' then
      split_part(path, '/', 3) = auth.jwt() ->> 'sub'
      and path ~ '^ecos/[A-Za-z0-9]{20}/[^/]+/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.(jpg|m4a)$'
    when 'stories' then
      split_part(path, '/', 2) = auth.jwt() ->> 'sub'
      and path ~ '^stories/[^/]+/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.(jpg|mp4)$'
    else false
  end
$$;

drop policy if exists "nexus media insert" on storage.objects;
drop policy if exists "nexus media delete" on storage.objects;
drop policy if exists "nexus media select own" on storage.objects;

-- Storage looks the object up before deleting it, so the owner needs to "see" their files.
-- Everyone else reads through the public URL, which does not go through these policies
create policy "nexus media select own" on storage.objects
  for select to anon, authenticated
  using (bucket_id = 'media' and public.is_nexus_user() and public.is_own_media_path(name));

-- Create only: there is no update policy, so a file can never be overwritten
create policy "nexus media insert" on storage.objects
  for insert to anon, authenticated
  with check (bucket_id = 'media' and public.is_nexus_user() and public.is_own_media_path(name));

-- Cleanup of a post, eco or story that failed after its upload
create policy "nexus media delete" on storage.objects
  for delete to anon, authenticated
  using (bucket_id = 'media' and public.is_nexus_user() and public.is_own_media_path(name));
