import { extractPublicId } from './scanTarget';

describe('extractPublicId', () => {
  it('reads the id out of the guide URL the app’s own codes carry', () => {
    expect(extractPublicId('http://localhost:8081/q/3zhmodkfwk')).toBe('3zhmodkfwk');
    expect(extractPublicId('https://gema.app/q/aezpr29esc')).toBe('aezpr29esc');
  });

  it('tolerates a trailing slash, query string or fragment', () => {
    expect(extractPublicId('https://gema.app/q/3zhmodkfwk/')).toBe('3zhmodkfwk');
    expect(extractPublicId('https://gema.app/q/3zhmodkfwk?utm=cartao')).toBe('3zhmodkfwk');
    expect(extractPublicId('https://gema.app/q/3zhmodkfwk#topo')).toBe('3zhmodkfwk');
  });

  it('accepts a bare id, which is what someone types off a printed card', () => {
    expect(extractPublicId('3zhmodkfwk')).toBe('3zhmodkfwk');
    expect(extractPublicId('  3zhmodkfwk  ')).toBe('3zhmodkfwk');
  });

  it('accepts a guide URL from another origin', () => {
    // A printed card outlives a domain change, and the id is what identifies
    // the plan — not the host it was minted under.
    expect(extractPublicId('https://outro-dominio.example/q/3zhmodkfwk')).toBe('3zhmodkfwk');
  });

  it('rejects a code that is not a GEMA plan', () => {
    // Scanning the wrong thing by accident should say so, not send the app off
    // to fetch nonsense.
    expect(extractPublicId('https://example.com')).toBeNull();
    expect(extractPublicId('WIFI:S:casa;T:WPA;P:senha;;')).toBeNull();
    expect(extractPublicId('tel:5199999000')).toBeNull();
    expect(extractPublicId('')).toBeNull();
    expect(extractPublicId('   ')).toBeNull();
  });

  it('rejects an id of the wrong shape', () => {
    expect(extractPublicId('CURTO')).toBeNull();
    expect(extractPublicId('3zhmodkfw')).toBeNull(); // nine characters
    expect(extractPublicId('3zhmodkfwkk')).toBeNull(); // eleven
    expect(extractPublicId('3ZHMODKFWK')).toBeNull(); // uppercase is not minted
  });

  it('does not let a scanned string smuggle in extra path', () => {
    // The id is taken on its own; whatever follows a well-formed separator is
    // discarded rather than travelling with it.
    expect(extractPublicId('https://gema.app/q/3zhmodkfwk/../admin')).toBe('3zhmodkfwk');
    // And an id that runs straight into something else is not a well-formed
    // guide URL at all, so it is refused outright.
    expect(extractPublicId('/q/3zhmodkfwk%2F..')).toBeNull();
    expect(extractPublicId('/q/3zhmodkfwkextra')).toBeNull();
  });
});
