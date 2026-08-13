/**
 * A GEMA public id: ten lowercase alphanumerics, as minted by the backend.
 *
 * Anchored, because this decides what gets fetched — a loose match would let a
 * scanned string smuggle in path segments or a query string.
 */
const PUBLIC_ID = /^[a-z0-9]{10}$/;

/** `/q/{publicId}` anywhere in a URL's path, which is the shape the QR encodes. */
const GUIDE_PATH = /\/q\/([a-z0-9]{10})(?:[/?#]|$)/;

/**
 * Reads a plan's public id out of whatever a camera just decoded.
 *
 * Accepts the guide URL the app's own codes carry, and a bare id, which is what
 * someone reads off a printed card when the camera cannot get a lock. Anything
 * else — a website, a wifi barcode, another product's QR — returns null, so an
 * unrelated code scanned by accident says so instead of sending the app off to
 * fetch nonsense.
 *
 * Deliberately does not check the URL's host: a plan's guide may be served from
 * a different origin than the one this build points at (a printed card outlives
 * a domain change), and the id is the part that identifies the plan.
 */
export function extractPublicId(scanned: string): string | null {
  const value = scanned.trim();
  if (!value) return null;

  if (PUBLIC_ID.test(value)) return value;

  const inPath = value.match(GUIDE_PATH);
  if (inPath) return inPath[1];

  return null;
}
