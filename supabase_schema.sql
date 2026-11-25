-- ============================================================
--  거미줄 서점 · Supabase 스키마
--  Supabase 대시보드 > SQL Editor 에 통째로 붙여넣고 Run 하세요. (여러 번 실행해도 안전)
-- ============================================================

-- 회원 (일반 가입 + 카카오 로그인 회원 + 쿠폰 상태)
create table if not exists app_users (
  user_id        text primary key,            -- 일반: 입력한 아이디 / 카카오: 'kakao_<카카오회원번호>'
  password_hash  text not null default '',    -- SHA-256 (카카오 회원은 빈 값)
  name           text not null,
  phone          text not null default '',
  address        text,
  join_date      text,
  provider       text not null default 'local',   -- 'local' | 'kakao'
  coupon_code    text,                        -- 등록한 쿠폰 코드 (없으면 null)
  coupon_used    boolean not null default false,
  created_at     timestamptz not null default now()
);

-- 장바구니 (서버를 꺼도 유지). 도서 정보를 같이 저장해서 카카오 도서도 복원됩니다.
create table if not exists cart_items (
  user_id       text not null references app_users(user_id) on delete cascade,
  book_id       text not null,
  title         text not null,
  author        text,
  price         integer not null,
  image         text,
  category      text,
  description   text,
  release_date  text,
  quantity      integer not null default 1 check (quantity > 0),
  added_at      timestamptz not null default now(),
  primary key (user_id, book_id)
);

-- 주문 (주문한 시각 = ordered_at)
create table if not exists orders (
  order_id          text primary key,
  user_id           text not null,
  user_name         text,
  recipient_name    text,
  recipient_phone   text,
  shipping_date     text,
  shipping_address  text,
  ordered_at        timestamptz not null default now(),
  status            text not null default '주문완료',
  coupon_code       text,
  discount          integer not null default 0,
  total_amount      integer not null default 0
);
create index if not exists orders_user_idx on orders (user_id, ordered_at desc);

-- 주문 상세 (주문 당시 도서명/가격 스냅샷)
create table if not exists order_items (
  id        bigserial primary key,
  order_id  text not null references orders(order_id) on delete cascade,
  book_id   text,
  title     text not null,
  price     integer not null,
  quantity  integer not null,
  image     text
);
create index if not exists order_items_order_idx on order_items (order_id);

-- 보안: RLS 를 켜고 정책을 만들지 않으면 anon 키로는 접근 불가.
-- 서버(Tomcat)가 쓰는 service_role 키는 RLS 를 우회하므로 정상 동작합니다.
alter table app_users   enable row level security;
alter table cart_items  enable row level security;
alter table orders      enable row level security;
alter table order_items enable row level security;
