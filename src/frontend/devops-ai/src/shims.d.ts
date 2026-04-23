declare module "*.module.css" {
  const classes: Record<string, string>;
  export default classes;
}

declare module "*.css" {}

declare module "*.svg" {
  const src: string;
  export default src;
}

declare module "*.png" {
  const src: string;
  export default src;
}
