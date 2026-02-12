import React from 'react';
import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';

import Attendance from './Attendance';

describe('Attendance', () => {
  it('renders placeholder UI', () => {
    render(<Attendance />);

    expect(screen.getByRole('heading', { name: /attendance/i })).toBeInTheDocument();
    expect(screen.getByText(/ui not implemented yet/i)).toBeInTheDocument();
  });
});
